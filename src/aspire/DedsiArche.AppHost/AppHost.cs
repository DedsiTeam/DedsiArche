using Microsoft.Extensions.Configuration;

var builder = DistributedApplication.CreateBuilder(args);
var config = builder.Configuration;

// Aspire 配置独立于 Java 的 .env；端口、路径和应用参数统一由 appsettings.json 提供。
int Port(string name)
{
    var port = config.GetValue<int>($"Ports:{name}");
    return port is > 0 and <= 65535
        ? port
        : throw new InvalidOperationException($"请在 appsettings.json 中配置有效端口 Ports:{name}。");
}
string Required(string key) => !string.IsNullOrWhiteSpace(config[key])
    ? config[key]!
    : throw new InvalidOperationException($"appsettings.json 缺少配置 {key}。");

var postgresPort = Port("Postgres");
var redisPort = Port("Redis");
var backendPort = Port("Backend");
var frontendPort = Port("Frontend");
config["ASPNETCORE_URLS"] = $"http://localhost:{Port("Dashboard")}";
config["ASPIRE_DASHBOARD_OTLP_ENDPOINT_URL"] = $"http://localhost:{Port("Otlp")}";
config["ASPIRE_RESOURCE_SERVICE_ENDPOINT_URL"] = $"http://localhost:{Port("ResourceService")}";
config["ASPIRE_ALLOW_UNSECURED_TRANSPORT"] = "true";

var javaRoot = Path.GetFullPath(Path.Combine(builder.AppHostDirectory, "../../java"));
var reactRoot = Path.GetFullPath(Path.Combine(builder.AppHostDirectory, "../../react"));
const string jar = "dedsi-arche-host/target/dedsi-arche-host-1.0.0-SNAPSHOT.jar";
if (!File.Exists(Path.Combine(javaRoot, jar)))
    throw new InvalidOperationException("后端 JAR 不存在，请使用 src/aspire/start.ps1 构建并启动。");

var postgresData = Required("PostgreSQL:DataDirectory");
var redisData = Required("Redis:DataDirectory");
RequireDataFile(postgresData, Required("PostgreSQL:DataFile"));
RequireDataFile(redisData, Required("Redis:DataFile"));
var databaseName = Required("PostgreSQL:Database");
var contextPath = Required("Backend:ContextPath");
var databaseUrl = $"jdbc:postgresql://localhost:{postgresPort}/{databaseName}";
var apiBaseUrl = $"http://localhost:{backendPort}{contextPath}";
var origins = config.GetSection("Backend:AllowedOrigins").Get<string[]>()
    ?? throw new InvalidOperationException("请配置 Backend:AllowedOrigins。");
var allowedOrigins = string.Join(",", origins.Select(origin =>
    origin.Replace("{FrontendPort}", frontendPort.ToString())));

#region 参数
var postgresUser = builder.AddParameter("postgres-user");
var postgresPassword = builder.AddParameter("postgres-password", secret: true);
var redisPassword = builder.AddParameter("redis-password", secret: true);
var jwtSecret = builder.AddParameter("jwt-secret", secret: true);
#endregion

#region 基础设施
var postgres = builder.AddPostgres("dedsi-arche-postgres", userName: postgresUser, password: postgresPassword, port: postgresPort)
    .WithImageTag(Required("PostgreSQL:ImageTag"))
    // PostgreSQL 18 挂载整个 /var/lib/postgresql，保留 18/docker 目录结构。
    .WithDataBindMount(postgresData)
    .WithContainerName("dedsiarche-aspire-postgres")
    .WithLifetime(ContainerLifetime.Persistent)
    .WithEndpoint("tcp", endpoint => endpoint.IsProxied = false);
var database = postgres.AddDatabase("dedsi-arche-db", databaseName);

#pragma warning disable ASPIRECERTIFICATES001
var redis = builder.AddRedis("dedsi-arche-redis", port: redisPort, password: redisPassword)
    // 与 Java 的本机 Redis 密码连接协议一致。
    .WithoutHttpsCertificate()
    .WithImageTag(Required("Redis:ImageTag"))
    .WithDataBindMount(redisData)
    .WithArgs("--appendonly", "yes")
    .WithContainerName("dedsiarche-aspire-redis")
    .WithLifetime(ContainerLifetime.Persistent)
    .WithEndpoint("tcp", endpoint => endpoint.IsProxied = false);
#pragma warning restore ASPIRECERTIFICATES001
#endregion

#region Java 后端
var backend = builder.AddExecutable("dedsi-arche-host", "java", javaRoot, "-jar", jar)
    .WithHttpEndpoint(port: backendPort, targetPort: backendPort, env: "SERVER_PORT", isProxied: false)
    // 覆盖 application.yml 中的可选 .env 导入，独立 Java 启动仍使用原配置。
    .WithEnvironment("SPRING_CONFIG_IMPORT", "")
    .WithEnvironment("SERVER_SERVLET_CONTEXT_PATH", contextPath)
    .WithEnvironment("DB_URL", databaseUrl)
    .WithEnvironment("DB_USER", postgresUser)
    .WithEnvironment("DB_PASSWORD", postgresPassword)
    .WithEnvironment("REDIS_HOST", "localhost")
    .WithEnvironment("REDIS_PORT", redisPort.ToString())
    .WithEnvironment("REDIS_PASSWORD", redisPassword)
    .WithEnvironment("JWT_SECRET", jwtSecret)
    .WithEnvironment("USER_ADMIN_USERNAMES", config["Backend:AdminUsernames"] ?? "")
    .WithEnvironment("CORS_ALLOWED_ORIGINS", allowedOrigins)
    .WithEnvironment("DOCS_ENABLED", config.GetValue<bool>("Backend:DocsEnabled").ToString().ToLowerInvariant())
    .WithHttpHealthCheck($"{contextPath}/actuator/health")
    .WaitFor(database)
    .WaitFor(redis);
#endregion

#region React 前端
builder.AddExecutable("dedsi-arche-react", "bun", reactRoot,
        "run", "dev", "--host", "localhost", "--port", frontendPort.ToString(), "--strictPort")
    .WithHttpEndpoint(port: frontendPort, targetPort: frontendPort, isProxied: false)
    .WithEnvironment("VITE_API_BASE_URL", apiBaseUrl)
    .WaitFor(backend);
#endregion

builder.Build().Run();

static void RequireDataFile(string directory, string relativeFile)
{
    if (!Directory.Exists(directory) || !File.Exists(Path.Combine(directory, relativeFile)))
        throw new InvalidOperationException($"缺少现有数据文件：{Path.Combine(directory, relativeFile)}。请恢复迁移数据，不会自动创建空库。");
}