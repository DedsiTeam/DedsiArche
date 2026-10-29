# 本地 Aspire 编排

参考 DedsiNative.AppHost 的参数、基础设施与应用分区方式，使用 Aspire 13.6.4 / .NET 10。编排 PostgreSQL、Redis、Java 后端与 React 前端（Bun 启动）。

## 启动

前置条件：Docker Desktop（Linux 容器）、.NET 10 SDK、JDK 26、全局 Maven、Bun；准备 `DedsiArche.AppHost/appsettings.json`。使用 C:\Dockers 中已迁移的数据，不初始化新的开发库。

从仓库根目录执行：

```powershell
.\src\aspire\start.ps1
```

入口检查迁移后的数据文件和端口，安装前端依赖、构建 AppHost 和后端（跳过测试），随后启动 Aspire。已运行的 Aspire 或 IDE 后端需要先手动停止。脚本使用 UTF-8 BOM 编码，兼容 Windows PowerShell 5.1。

| 资源 | 地址/端口 |
| --- | --- |
| Aspire Dashboard | http://localhost:11519（使用启动日志提供的登录链接） |
| React 前端 | http://localhost:11126 |
| Java API | http://localhost:11518/dedsi-arche-api |
| Scalar 文档 | http://localhost:11518/dedsi-arche-api/scalar |
| PostgreSQL | localhost:25473，数据库 dedsi_arche，用户 dedsi |
| Redis | localhost:28641 |

Dashboard 的 OTLP 和资源服务分别使用本机 11520、11521。Dashboard 保留默认认证，不启用匿名访问。

## 数据与配置

- PostgreSQL 镜像固定 `postgres:18.6`，主机目录 `C:\Dockers\PostgreSql\DedsiArcheDB` 挂载到 `/var/lib/postgresql`，数据库文件位于其 `18/docker` 子目录。
- Redis 镜像固定 `redis:8.10.2`，主机目录 `C:\Dockers\Redis\DedsiArche` 挂载到 `/data`，继续启用 AOF。
- 参数统一在 `DedsiArche.AppHost/appsettings.json` 的 `Parameters` 节点配置，`AddParameter` 按名称读取。PostgreSQL 与 Redis 使用分别生成的 32 字节随机密码；JWT 沿用原密钥。实际配置已加入 Git 忽略，仓库提供 `appsettings.example.json`。首次在其他机器部署时需创建实际配置并使用对应数据库凭据。
- 本机 `src/java/.env` 已同步数据库密码，支持 IDEA 独立启动。后续更换 PostgreSQL 密码时需要同步修改数据库角色和配置，仅修改配置不会更新已有数据库密码。Aspire 的管理员名单、CORS、文档开关从 `Backend` 节点读取；Java 独立启动继续读取自己的 `.env`。
- 缺少 PostgreSQL 的 `18/docker/PG_VERSION` 或 Redis 的 `appendonlydir/appendonly.aof.manifest` 时拒绝启动，避免静默创建空库。修改目录只需更新 appsettings.json；AppHost 和启动脚本共同读取该配置。
- Aspire 容器使用 Persistent 生命周期，退出 AppHost 后数据库、Redis 容器继续运行，应用进程随 AppHost 退出。
- 不要让其他容器同时写入这些目录。不要删除数据目录、迁移备份或旧命名卷。

Java 使用 Aspire 稳定的 `AddExecutable` 接口启动 JVM 并运行预先打包的 JAR；沿用进程编排方式，不依赖 Java 集成预览包。修改 Java 源码后重新执行启动脚本；如果 AppHost 已运行，先 Ctrl+C 退出。AppHost 单独启动不会重新构建 Java。后端健康检查使用 Actuator，启动依赖通过 `WaitFor` 管理；控制台日志由 Aspire 展示，当前未额外接入 OpenTelemetry Java agent。

## 2026-10-29 数据迁移

数据库停止写入后，原卷以只读方式挂载，将全部文件离线归档并复制至上述目录，逐文件 SHA-256 对比一致。未运行功能测试。

迁移备份位于 `C:\Dockers\DedsiArcheBackups\migration-20261029-151726`，包含 `postgres.tar`、`redis.tar` 和复制前后的文件校验清单。

原卷 `dedsiarche_postgres-data`、`dedsiarche_redis-data` 保留。旧容器已停止并重命名为 `dedsiarche-postgres-volume-backup-20261029`、`dedsiarche-redis-volume-backup-20261029`；不要启动它们作为日常服务。原卷和归档仅包含迁移时的数据，新写入的数据只保存在 C:\Dockers 的新目录。

如需回退，先停止 Aspire 及新的两个数据库容器，备份新目录中的最新数据，再选择恢复源并同步调整挂载配置。直接切回旧卷会丢失迁移后的新增写入。

React 资源名为 `dedsi-arche-react`，启动入口先用 `bun install --frozen-lockfile` 安装锁定依赖，再由 Aspire 执行 `bun run dev`。保持 11126 严格端口，前端直接请求后端 11518，不使用 Vite 或 Aspire 代理。后端 CORS 仍只允许配置中的明确来源，默认 localhost:11126 和 127.0.0.1:11126，不开放通配符。前端等待后端健康后启动，退出 Aspire 时一并停止。
## 两种启动方式

### Aspire 统一启动

运行仓库根目录的 `src/aspire/start.ps1`。所有编排配置读取 AppHost 的 `appsettings.json`：

- `Parameters`：数据库用户名、数据库/Redis 密码和 JWT 密钥。
- `Ports`：PostgreSQL、Redis、Java、React、Dashboard、OTLP、资源服务端口；各端口只定义一次。
- `PostgreSQL` / `Redis`：镜像版本、数据目录和启动前必须存在的数据文件。
- `Backend`：接口路径、管理员名单、文档开关和允许的跨域来源。来源中的 `{FrontendPort}` 自动替换为配置的前端端口。

Aspire 不读取 Java 的 .env，并为 Java 子进程覆盖可选 .env 导入。端口及挂载目录检查保留，配置缺失时拒绝启动。

### 原有 Java 独立启动

运行 `src/scripts/run-local.ps1`，或使用 IDEA 启动后端。继续使用 `src/java/.env` 和 `application.yml`，不读取 Aspire 的 appsettings.json。原启动脚本无需改动。

两种方式不能同时占用相同后端端口。独立启动 Java 前，先停止 Aspire 后端；PostgreSQL 和 Redis 使用持久容器，仍可继续提供服务。若修改基础设施端口或密码，需要同步 Java 独立启动的 DB_URL、DB_USER、DB_PASSWORD、REDIS_HOST、REDIS_PORT、REDIS_PASSWORD；两份配置不自动互相覆盖。