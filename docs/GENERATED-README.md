# 模块化单体 Web API

Java 26 / Spring Boot 4.1.2 / Spring Data JPA + Hibernate / PostgreSQL / Spring Security JWT / Redis。

## 本地启动（PowerShell）

准备 JDK 26，令 JAVA_HOME 指向 JDK 目录，启动 Docker Desktop。

```powershell
.\scripts\init-local.ps1
docker compose up -d --wait
.\scripts\run-local.ps1
```

打开另一个终端执行 `.\scripts\smoke-test.ps1` 验证。脚本会新增一个随机测试用户。
健康检查：`http://localhost:8080/actuator/health`。
构建测试：`.\mvnw.cmd clean verify`，默认 H2 测试无需 Docker。
停止依赖：`docker compose stop`。`docker compose down` 保留数据卷；不要随意添加 `-v`。

## 分层

一个 Host + Common + System 业务模块。每个业务模块按 `api/domain/infrastructure` 分层，每层下按 `users` 等领域组织。
API 负责事务、用例编排和 DTO 转换；Domain 放实体、仓储契约及 Query/Dto；Infrastructure 实现仓储、查询和安全技术。
无 Application、DO、ApiMapper。领域实体使用 JPA 注解，Spring Data 仓储提供基础 CRUD。
事务内修改受管理实体无需显式 save，新增实体仍需 persist/save。关闭 OSIV，不向客户端序列化实体。

## User API

| 方法 | 路径 | 用途 |
|---|---|---|
| POST | /api/users/register | username、displayName、password 注册 |
| POST | /api/users/login | username、password 登录 |
| POST | /api/users/refresh | refreshToken 换取新令牌 |
| POST | /api/users/logout | refreshToken 注销刷新会话 |
| GET | /api/users/me | Bearer JWT 查询自己 |
| PATCH | /api/users/me | Bearer JWT 修改 displayName |

用户名为 3～50 位英文字母、数字、下划线，统一小写。密码 12～64 字符，UTF-8 最多 72 字节，BCrypt 存储。
JWT 有效期 15 分钟；刷新令牌有效期 7 天，数据库仅存 SHA-256，刷新时旧令牌撤销且新令牌生成在同一事务内。
并发刷新通过数据库悲观锁实现只消费一次。注销不立即撤销已签发 JWT，其在过期前仍有效。
认证入口公开；当前用户接口要求 profile scope。没有默认账号、管理员后门或角色管理功能。
此模板不内置登录限流，公开部署前应在网关增加限流并评估注册是否开放。令牌使用 HTTPS 传输，避免写入日志。
客户端令牌通过响应体返回；若改为 Cookie 传递凭据，需重新设计 CSRF 策略。

## 数据与 Redis

Flyway 创建 sys_users 和认证内部表 sys_refresh_sessions，Hibernate 仅 validate，禁止自动修改生产表结构。
Redis 已配置连接、独立密码与 Actuator 健康检查；当前用户查询直接读取数据库，不缓存密码或认证决策。
数据库与 Redis 仅映射至 127.0.0.1 的 54329、63799。不同项目并行运行时调整 compose 端口及 DB_URL/REDIS_PORT。
数据库时间使用 Instant / timestamptz，API 输出 ISO-8601 UTC。没有复用其他仓库的本地时间规则。
刷新会话过期记录需要部署环境定期清理（仅删除已过期记录）；示例未启用后台清理任务。

## 新增业务模块

```powershell
.\scripts\add-module.ps1 -Name orders
.\mvnw.cmd verify
```

脚本创建模块、更新父 POM 和 Host 依赖。模块内部各层继续按领域建立包。新增 Flyway 迁移目录时，在 Host 的 spring.flyway.locations 登记，迁移版本在整个应用中保持唯一。
业务模块不直接操作另一个模块的仓储，也不建立跨模块 JPA 实体关联。

## 密钥

`.env` 由 init-local.ps1 随机生成，已加入 .gitignore。Maven/IDE 不自动读取 .env；run-local.ps1 将其加载到进程环境。
IDE 运行需要设置 DB_PASSWORD、REDIS_PASSWORD、JWT_SECRET。不要把 .env 提交或放入模板包。
Redis 密码、数据库密码更换后需同步更新现有实例；已有 PostgreSQL 数据卷不会因环境变量变化自动重设密码。

## Scalar 接口文档

启动 Host 后访问 `http://localhost:8080/scalar`，OpenAPI JSON 位于 `/v3/api-docs`，System 分组位于 `/v3/api-docs/system`。
通过页面调用注册和登录接口，复制返回的 `accessToken`，填入 Scalar 的 Bearer 认证框，再调试 `/api/users/me`；不要重复填写 `Bearer ` 前缀。
默认开启文档，设置环境变量 `DOCS_ENABLED=false` 可同时关闭页面与 OpenAPI 端点。
采用 springdoc-openapi 3.2.0 的 WebMVC Scalar Starter，不使用 Swagger UI。
Host 定义标题和认证方案，业务模块在 Controller/DTO 上声明中文说明，并通过 GroupedOpenApi 注册分组。
新增业务模块的接口自动出现在全量文档中；需要独立分组时参考 UserOpenApiConfiguration。
