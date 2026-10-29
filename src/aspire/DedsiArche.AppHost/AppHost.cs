using System.Diagnostics;

var builder = DistributedApplication.CreateBuilder(args);
var javaRoot = Path.GetFullPath(Path.Combine(builder.AppHostDirectory, "../../java"));
var envPath = Path.Combine(javaRoot, ".env");
if (!File.Exists(envPath))
    throw new InvalidOperationException("缺少 src/java/.env，请保留并使用现有数据库凭据。");

// 只读取所需配置，不输出密钥，不覆盖现有 .env。
var localEnv = new Dictionary<string, string>(StringComparer.Ordinal);
foreach (var line in File.ReadLines(envPath))
{
    var entry = line.Trim();
    if (entry.Length == 0 || entry.StartsWith('#')) continue;
    var separator = entry.IndexOf('=');
    if (separator > 0) localEnv[entry[..separator].Trim()] = entry[(separator + 1)..];
}
string Setting(string name, string? fallback = null) =>
    Environment.GetEnvironmentVariable(name) ?? localEnv.GetValueOrDefault(name) ?? fallback
    ?? throw new InvalidOperationException($"缺少配置 {name}，请检查 src/java/.env。");

// 固定已存在的卷，拒绝在卷丢失时悄悄初始化空数据库。
RequireExistingVolume("dedsiarche_postgres-data", "dedsiarche-aspire-postgres");
RequireExistingVolume("dedsiarche_redis-data", "dedsiarche-aspire-redis");
const string jar = "dedsi-arche-host/target/dedsi-arche-host-1.0.0-SNAPSHOT.jar";
if (!File.Exists(Path.Combine(javaRoot, jar)))
    throw new InvalidOperationException("后端 JAR 不存在，请使用 src/aspire/start.ps1 构建并启动。");

#region 参数
builder.Configuration["Parameters:postgres-user"] = "dedsi";
builder.Configuration["Parameters:postgres-password"] = Setting("DB_PASSWORD");
builder.Configuration["Parameters:redis-password"] = Setting("REDIS_PASSWORD");
builder.Configuration["Parameters:jwt-secret"] = Setting("JWT_SECRET");
var postgresUser = builder.AddParameter("postgres-user");
var postgresPassword = builder.AddParameter("postgres-password", secret: true);
var redisPassword = builder.AddParameter("redis-password", secret: true);
var jwtSecret = builder.AddParameter("jwt-secret", secret: true);
#endregion

#region 基础设施
var postgres = builder.AddPostgres("dedsi-arche-postgres", userName: postgresUser, password: postgresPassword, port: 54329)
    .WithImageTag("18.6")
    // PostgreSQL 18 挂载 /var/lib/postgresql，与现有 Compose 一致。
    .WithDataVolume("dedsiarche_postgres-data")
    .WithContainerName("dedsiarche-aspire-postgres")
    .WithLifetime(ContainerLifetime.Persistent)
    .WithEndpoint("tcp", endpoint => endpoint.IsProxied = false);
var database = postgres.AddDatabase("dedsi-arche-db", "dedsi_arche");

#pragma warning disable ASPIRECERTIFICATES001
// 保持原有本机密码连接协议，与 Java 的 Redis 配置一致。
var redis = builder.AddRedis("dedsi-arche-redis", port: 63799, password: redisPassword)
    .WithoutHttpsCertificate()
    .WithImageTag("8.10.2")
    .WithDataVolume("dedsiarche_redis-data")
    // 继续读取原有 AOF，不能仅启用快照持久化。
    .WithArgs("--appendonly", "yes")
    .WithContainerName("dedsiarche-aspire-redis")
    .WithLifetime(ContainerLifetime.Persistent)
    .WithEndpoint("tcp", endpoint => endpoint.IsProxied = false);
#endregion

#pragma warning restore ASPIRECERTIFICATES001
#region Java 后端
// 多模块项目由 start.ps1 使用现有 Maven 构建，再作为 JVM 进程运行。
var backend = builder.AddExecutable("dedsi-arche-host", "java", javaRoot, "-jar", jar)
    .WithHttpEndpoint(port: 11518, targetPort: 11518, env: "SERVER_PORT", isProxied: false)
    .WithEnvironment("DB_URL", "jdbc:postgresql://localhost:54329/dedsi_arche")
    .WithEnvironment("DB_USER", postgresUser)
    .WithEnvironment("DB_PASSWORD", postgresPassword)
    .WithEnvironment("REDIS_HOST", "localhost")
    .WithEnvironment("REDIS_PORT", "63799")
    .WithEnvironment("REDIS_PASSWORD", redisPassword)
    .WithEnvironment("JWT_SECRET", jwtSecret)
    .WithEnvironment("USER_ADMIN_USERNAMES", Setting("USER_ADMIN_USERNAMES", ""))
    .WithEnvironment("CORS_ALLOWED_ORIGINS", Setting("CORS_ALLOWED_ORIGINS", "http://localhost:11126,http://127.0.0.1:11126"))
    .WithEnvironment("DOCS_ENABLED", Setting("DOCS_ENABLED", "true"))
    .WithHttpHealthCheck("/dedsi-arche-api/actuator/health")
    .WaitFor(database)
    .WaitFor(redis);
#endregion

#region React 前端
var reactRoot = Path.GetFullPath(Path.Combine(builder.AppHostDirectory, "../../react"));
builder.AddExecutable("dedsi-arche-react", "bun", reactRoot,
        "run", "dev", "--host", "localhost", "--port", "11126", "--strictPort")
    .WithHttpEndpoint(port: 11126, targetPort: 11126, isProxied: false)
    .WithEnvironment("VITE_API_BASE_URL", "http://localhost:11518/dedsi-arche-api")
    .WaitFor(backend);
#endregion

builder.Build().Run();

static void RequireExistingVolume(string volume, string allowedContainer)
{
    RunDocker("volume", "inspect", volume, "--format", "{{.Name}}");
    var holders = RunDocker("ps", "--filter", $"volume={volume}", "--format", "{{.Names}}");
    foreach (var name in holders.Split('\n', StringSplitOptions.TrimEntries | StringSplitOptions.RemoveEmptyEntries))
        if (name != allowedContainer)
            throw new InvalidOperationException($"数据卷 {volume} 正被 {name} 使用，请通过 src/aspire/start.ps1 切换，禁止同时运行两套数据库。");
}

static string RunDocker(params string[] arguments)
{
    var info = new ProcessStartInfo("docker") { RedirectStandardOutput = true, RedirectStandardError = true, UseShellExecute = false, CreateNoWindow = true };
    foreach (var argument in arguments) info.ArgumentList.Add(argument);
    using var process = Process.Start(info) ?? throw new InvalidOperationException("无法启动 Docker。");
    var output = process.StandardOutput.ReadToEndAsync();
    var error = process.StandardError.ReadToEndAsync();
    process.WaitForExit();
    Task.WaitAll(output, error);
    if (process.ExitCode != 0)
        throw new InvalidOperationException("Docker 检查失败：请启动 Docker Desktop 并确认现有数据卷存在；不会自动创建替代卷。");
    return output.Result.Trim();
}
