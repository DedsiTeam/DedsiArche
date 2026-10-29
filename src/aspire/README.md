# 本地 Aspire 编排

参考 DedsiNative.AppHost 的参数、基础设施与应用分区方式，使用 Aspire 13.6.4 / .NET 10。编排 PostgreSQL、Redis、Java 后端与 React 前端（Bun 启动）。

## 启动

前置条件：Docker Desktop（Linux 容器）、.NET 10 SDK、JDK 26、全局 Maven、Bun；保留 `src/java/.env`。使用已有数据卷，不初始化新的开发库。

从仓库根目录执行：

```powershell
.\src\aspire\start.ps1
```

入口先检查卷和进程并构建 AppHost；首次切换时备份数据库，停止本仓库旧 Java JAR 进程后构建后端（跳过测试），再停止正在使用这些卷的旧 Compose 容器并启动 Aspire。IDE 启动的后端需要先手动停止。

| 资源 | 地址/端口 |
| --- | --- |
| Aspire Dashboard | http://localhost:11519（使用启动日志提供的登录链接） |
| React 前端 | http://localhost:11126 |
| Java API | http://localhost:11518/dedsi-arche-api |
| Scalar 文档 | http://localhost:11518/dedsi-arche-api/scalar |
| PostgreSQL | localhost:54329，数据库 dedsi_arche，用户 dedsi |
| Redis | localhost:63799 |

Dashboard 的 OTLP 和资源服务分别使用本机 11520、11521。Dashboard 保留默认认证，不启用匿名访问。

## 数据与配置

- PostgreSQL 镜像固定 `postgres:18.6`，复用 `dedsiarche_postgres-data`，挂载 `/var/lib/postgresql`。
- Redis 镜像固定 `redis:8.10.2`，复用 `dedsiarche_redis-data`，挂载 `/data`，继续启用 AOF。
- 密码和 JWT 密钥从现有 `src/java/.env` 读取，进程环境变量可覆盖；不重新生成凭据。不要将旧库密码改成新的随机密码。
- 数据卷缺失时启动会报错，避免静默创建空库；使用其他 Compose 项目名时需同步修改 AppHost 和启动脚本的卷与容器名称。
- 旧 Compose 容器只停止，不删除。首次切换的数据库备份位于 `.local/backups`，该目录不提交 Git。
- Aspire 容器使用 Persistent 生命周期，退出 AppHost 后数据库、Redis 容器继续运行，后端随 AppHost 退出。重启 AppHost 会继续复用它们。
- 不要同时启动 Compose 和 Aspire 两套数据库容器，也不要执行 `docker compose down -v` 或删除数据卷。

Java 使用 Aspire 稳定的 `AddExecutable` 接口启动 JVM 并运行预先打包的 JAR；沿用进程编排方式，不依赖 Java 集成预览包。修改 Java 源码后重新执行启动脚本；如果 AppHost 已运行，先 Ctrl+C 退出。AppHost 单独启动不会重新构建 Java。后端健康检查使用 Actuator，启动依赖通过 `WaitFor` 管理；控制台日志由 Aspire 展示，当前未额外接入 OpenTelemetry Java agent。

## 启动失败时恢复原有容器

先 Ctrl+C 退出 Aspire，再执行：

```powershell
docker stop dedsiarche-aspire-postgres dedsiarche-aspire-redis
cd src/java
docker start dedsiarche-postgres-1 dedsiarche-redis-1
..\scripts\run-local.ps1
```

此恢复方式要求原有容器仍存在；两个容器使用同一数据卷，无需导入或重建数据库。仓库已删除 Compose 配置，日常统一使用 Aspire 启动。

已按用户要求构建并启动 Aspire，未执行自动化或端到端测试。

React 资源名为 `dedsi-arche-react`，启动入口先用 `bun install --frozen-lockfile` 安装锁定依赖，再由 Aspire 执行 `bun run dev`。保持 11126 严格端口，前端直接请求后端 11518，不使用 Vite 或 Aspire 代理。后端 CORS 仍只允许配置中的明确来源，默认 localhost:11126 和 127.0.0.1:11126，不开放通配符。前端等待后端健康后启动，退出 Aspire 时一并停止。