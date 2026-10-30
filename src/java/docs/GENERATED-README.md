# 模块化单体 Web API

Java 26 / Spring Boot 4.1.2 / Spring Data JPA + Hibernate / PostgreSQL / Spring Security JWT / Redis。

## 本地启动（PowerShell）

以下命令针对模板生成的独立项目，在包含父 `pom.xml` 和 `scripts` 的项目根目录执行。在 DedsiArche 源仓库中，先执行 `cd src/java`，并将下文命令中的 `.\scripts\` 替换为 `..\scripts\`，因为源仓库的公共脚本位于 `src/scripts`。

准备 JDK 26，令 JAVA_HOME 指向 JDK 目录。安装全局 Maven（当前验证版本 3.10.0），将其 bin 目录加入 PATH，使用 `mvn --version` 确认安装成功，启动 Docker Desktop。

```powershell
.\scripts\init-local.ps1
# 先准备 PostgreSQL、Redis，并在 .env 中设置连接地址和现有密码
.\scripts\run-local.ps1
```

打开另一个终端，进入同一后端根目录后执行 `.\scripts\smoke-test.ps1` 验证。脚本会新增一个随机测试用户。如果设置了 `SERVER_PORT`，使用 `-BaseUrl` 指定对应地址。
健康检查：`http://localhost:11518/dedsi-arche-api/actuator/health`。
构建测试：`mvn clean verify`，默认 H2 测试无需 Docker。
源仓库使用 src/aspire/start.ps1 统一启动；模板生成的项目使用自行提供的 PostgreSQL、Redis，不再生成 Compose 文件。停止依赖时请保留其数据卷。

## 分层

一个 Host + Common + System 业务模块。每个业务模块按 `api/domain/infrastructure` 分层，每层下按 `users` 等领域组织。
API 负责事务、用例编排和 DTO 转换；Domain 放实体、仓储契约及 Query/Dto；Infrastructure 实现仓储、查询和安全技术。
无 Application、DO、ApiMapper。领域实体使用 JPA 注解，Spring Data 仓储提供基础 CRUD。
事务内修改受管理实体无需显式 save，新增实体仍需 persist/save。关闭 OSIV，不向客户端序列化实体。

## User API

| 方法 | 路径 | 用途 |
|---|---|---|
| POST | /dedsi-arche-api/api/users/register | username、displayName、password 注册 |
| POST | /dedsi-arche-api/api/users/login | username、password 登录 |
| POST | /dedsi-arche-api/api/users/refresh | refreshToken 换取新令牌 |
| POST | /dedsi-arche-api/api/users/logout | refreshToken 注销刷新会话 |
| GET | /dedsi-arche-api/api/users/me | Bearer JWT 查询自己 |
| PATCH | /dedsi-arche-api/api/users/me | Bearer JWT 修改 displayName |

用户名为 3～50 位英文字母、数字、下划线，统一小写。密码 12～64 字符，UTF-8 最多 72 字节，BCrypt 存储。
JWT 有效期 15 分钟；刷新令牌有效期 7 天，数据库仅存 SHA-256，刷新时旧令牌撤销且新令牌生成在同一事务内。
并发刷新通过数据库悲观锁实现只消费一次。注销不立即撤销已签发 JWT，其在过期前仍有效。
认证入口公开；当前用户接口要求 profile scope。没有默认账号、管理员后门或角色管理功能。
此模板不内置登录限流，公开部署前应在网关增加限流并评估注册是否开放。令牌使用 HTTPS 传输，避免写入日志。
客户端令牌通过响应体返回；若改为 Cookie 传递凭据，需重新设计 CSRF 策略。

## 数据与 Redis

Flyway 创建 sys_users 和认证内部表 sys_refresh_sessions，Hibernate 仅 validate，禁止自动修改生产表结构。

| 表或字段 | 说明 |
|---|---|
| `sys_users` | 用户聚合；`username` 具有唯一约束 |
| `password_hash` | BCrypt 密码哈希，不保存明文密码 |
| `enabled` | 控制用户能否登录和修改资料 |
| `created_at` | 用户注册时间，带时区 |
| `version` | Hibernate 乐观锁版本 |
| `sys_refresh_sessions` | 刷新会话；`user_id` 外键指向用户 |
| `token_hash` | 刷新令牌的唯一 SHA-256 摘要 |
| `expires_at` / `revoked` | 会话有效期及撤销状态 |
| 会话索引 | 按用户查会话、按过期时间清理记录 |

已执行的版本化 SQL 迁移保持原样，包括注释和排版，以免改变 Flyway 校验和。
表结构或数据库注释的后续变更应新增迁移版本。

Redis 已配置连接、独立密码与 Actuator 健康检查；当前用户查询直接读取数据库，不缓存密码或认证决策。
默认连接本机 PostgreSQL 25473 和 Redis 28641；不同项目并行运行时分别配置 DB_URL、REDIS_HOST 和 REDIS_PORT。
业务时间统一北京时间：Java 使用 LocalDateTime，数据库使用 timestamp without time zone，API 输出不带时区的 ISO 本地时间，前端直接展示。JWT 内部保持协议时间戳。
刷新会话过期记录需要部署环境定期清理（仅删除已过期记录）；示例未启用后台清理任务。

## 新增业务模块

```powershell
.\scripts\add-module.ps1 -Name orders
mvn verify
```

脚本创建模块、更新父 POM 和 Host 依赖。模块内部各层继续按领域建立包。新增 Flyway 迁移目录时，在 Host 的 spring.flyway.locations 登记，迁移版本在整个应用中保持唯一。
业务模块不直接操作另一个模块的仓储，也不建立跨模块 JPA 实体关联。

## 密钥

`.env` 由 init-local.ps1 随机生成，已加入 .gitignore。应用通过 spring.config.import 从工作目录或父目录读取 `.env`；run-local.ps1 也会将其加载到进程环境。
IDEA 可直接运行主类，工作目录设为后端根目录或 Host 模块目录。本仓库也提供 `DedsiArche Local` 配置，适用于直接打开 `src/java`；如果打开整个仓库，请导入 `src/java/pom.xml`，将运行配置工作目录设置为 `$PROJECT_DIR$/src/java` 并确认加载了 `src/java/.run` 中的配置。模板生成的独立项目直接使用其项目根目录。已有环境变量优先于 `.env`，请移除运行配置中过期的密码。不要把 .env 提交或放入模板包。
Redis 密码、数据库密码更换后需同步更新现有实例；已有 PostgreSQL 数据卷不会因环境变量变化自动重设密码。

## Scalar 接口文档

启动 Host 后访问 `http://localhost:11518/dedsi-arche-api/scalar`，OpenAPI JSON 位于 `/dedsi-arche-api/v3/api-docs`，System 分组位于 `/dedsi-arche-api/v3/api-docs/system`。
通过页面调用注册和登录接口，复制返回的 `accessToken`，填入 Scalar 的 Bearer 认证框，再调试 `/dedsi-arche-api/api/users/me`；不要重复填写 `Bearer ` 前缀。
默认开启文档，设置环境变量 `DOCS_ENABLED=false` 可同时关闭页面与 OpenAPI 端点。
采用 springdoc-openapi 3.2.0 的 WebMVC Scalar Starter，不使用 Swagger UI。
Host 定义标题和认证方案，业务模块在 Controller/DTO 上声明中文说明，并通过 GroupedOpenApi 注册分组。
新增业务模块的接口自动出现在全量文档中；需要独立分组时参考 UserOpenApiConfiguration。
