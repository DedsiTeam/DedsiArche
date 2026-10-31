---
name: dedsi-backend
description: 在 DedsiArche 中新增或修改 Java 后端领域、CRUD API、仓储、查询与 Flyway 迁移时使用。以现有 User 实现为结构和契约基准，落实中文注释、多行排版、I 前缀接口与 ULID 主键规范；不用于纯前端或纯 Aspire 运维任务。
---

# DedsiArche 后端开发

## 先确定基准，再写代码

从当前仓库根目录定位文件，不依赖固定机器路径。先遵守 [AGENTS.md](../../../AGENTS.md) 的 Trail 流程，读取 [后端约定](../../backend.md)、[工作流程](../../workflow.md) 和任务目录下适用的指令。涉及启动或配置时才读取 [运行指南](../../runtime.md)。这些指南是规则来源，用户当前明确要求优先。

修改前查看工作区状态。使用 Trail 定位下表中的当前实现，只读取本次需要的定义；图谱失效或未覆盖时说明后使用 rg 定位。不要根据记忆或本 skill 复制旧代码。

以下路径相对于 `src/java/dedsi-arche-system/src/main/java/com/dedsi/arche/system/`：

| 关注点 | User 参考实现 | 开始实现前必须确认 |
| --- | --- | --- |
| 领域模型 | `domain/users/User.java` | 实体映射、工厂方法、领域行为、JPA 构造器、时间字段、乐观锁 |
| 仓储契约 | `domain/users/IUserRepository.java` | 聚合读取与写入职责、Optional 语义 |
| 查询契约 | `domain/users/query/IUserQuery.java`、`UserPage.java`、`ManagedUserDto.java` | 只读投影、分页字段和默认值 |
| 持久化实现 | `infrastructure/users/repositories/JpaUserRepository.java`、`IUserJpaRepository.java` | 领域仓储适配器与 Spring Data 接口的分工 |
| 查询实现 | `infrastructure/users/queries/JpaUserQuery.java` | 参数绑定、投影查询、稳定排序、只读事务 |
| API 编排 | `api/users/UserManagementController.java`、`UserController.java` | 请求与响应组织、事务、状态码、版本检查、DTO 转换位置 |
| 请求与响应 | `api/users/request/`、`api/users/response/` | Bean Validation、中文 Schema 说明、空值与省略字段语义 |
| 异常与鉴权 | `api/users/UserExceptionHandler.java`、`infrastructure/users/security/UserAdministration.java` | 复用 ProblemDetail 和服务端管理权限检查 |
| 文档与迁移 | `infrastructure/users/docs/UserOpenApiConfiguration.java`；模块资源目录的 `db/migration/system/` | 文档分组覆盖、新迁移版本、既有数据兼容 |

在动手前给用户一条简短说明：本次复用哪些结构，哪些地方因需求不同而不同。尤其明确主键、更新请求的空值语义、接口方法和授权范围；不必为常规选择另行等待批准。

## 对齐方式

- 延续 `api / domain / infrastructure`；不额外引入 Application、DO、Mapper 或第二套异常/鉴权体系。DTO 转换位置先对照对应 User 用例，不自行换一种组织方式。
- 自定义接口统一 `I` 前缀，文件名一致，例如 `IOrderQuery.java`；实现类沿用 `JpaOrderQuery`、`JpaOrderRepository` 风格，Spring Data 接口为 `IOrderJpaRepository`。同步实现声明、import、构造注入、测试源码及文档引用，不改第三方接口名。
- `User` 保留 UUID，其他领域主键使用 26 位 ULID 字符串（Java String、数据库 varchar(26)）；因此不能直接复制 User 的 UUID 字段和生成方式。沿用项目已配置的 ULID 依赖，关联字段与主键类型一致。
- 业务时间统一北京时间 Asia/Shanghai，使用 LocalDateTime.now(ZoneId.of("Asia/Shanghai")) 获取，数据库使用 timestamp without time zone，API 使用无 Z、无时区偏移的 ISO 本地字符串，前端直接展示。不新增 UTC 兼容或按客户端时区转换的逻辑。JWT 标准 NumericDate 和认证数值计时仅在认证边界保留，不作为业务展示值；不要修改标准令牌语义。
- 领域字段、请求/响应字段、类和关键方法写中文注释，解释业务含义、默认值、不可变约束或副作用；API 的中文 OpenAPI 描述不能代替源码注释。字段、record 参数和枚举值旁的简短说明使用普通块注释 `/* 中文说明。 */`；类、接口和方法文档保留 Javadoc `/** ... */`。需要生成 record 文档时，在 record 声明的 Javadoc 中使用 `@param`，不在参数旁写 `/** ... */`。
- 方法体、record 字段列表使用多行格式；条件分支使用清晰的代码块，长参数列表与查询语句按逻辑换行。不要用压缩成单行的批量生成结果作为最终交付。
- User 中的旧排版不覆盖最新约定。发现疑似业务缺陷时，不将其照搬到新领域，也不未经授权顺手修复原模块。

## API 与数据必须核对的差异

- 优先遵循 REST 风格，资源定位优先使用路径参数，按语义选择 HTTP 方法；查询参数较多或条件复杂时使用 POST 和 JSON 请求 DTO，GET 不使用请求体。具体规则见后端指南的“接口与认证”。
- 同类管理操作优先沿用 User 的路由和 HTTP 语义。不要仅因个人偏好将编辑从 PATCH 换成 PUT；确需不同语义时说明理由并写清完整更新/部分更新及省略、null、空串的含义。已有接口按兼容性要求修改。
- 分页对照 User 的 `items / total / page / size`、起始页码、默认值与上限；新接口是否采用 POST body 按当前需求决定，不把用户分页约定推成所有接口必须 POST。
- 管理接口必须复用后端鉴权，不能靠前端隐藏入口或管理员标记。权限定义 CRUD 不自动等同于角色授权或按权限码判定访问，按用户授权范围交付。
- 写操作核对事务边界、唯一约束、版本冲突和返回值是否包含持久化后的版本；并发冲突不能只依赖写入前查询。避免将实体直接序列化为 API 响应。
- 复用现有错误处理，核对业务异常对应的 HTTP 状态与错误码，避免另起一套异常处理器。不得暴露底层数据库错误或凭据。
- Flyway 新增迁移，不修改已执行文件；字段类型、长度、空值、唯一键、索引与 JPA 保持一致。用户未要求的角色、菜单、数据范围等不自动加入新领域。
- 同步维护 `src/java/docs/sql/system/` 下按领域合并的完整结构快照：`users.sql` 包含用户与刷新会话，`permissions.sql` 包含权限，`managed_systems.sql` 包含系统管理。将增量变更折叠为最终 CREATE TABLE、约束、索引与中文数据库注释，注明截至哪个 Flyway 版本；保留原迁移作为执行入口。快照放在应用资源与 Flyway 扫描目录之外，仅供阅读参考，不执行到现有数据库，不自动修复迁移历史。
- 每张业务表和每个字段必须提供数据库可读取的中文说明：新建表或新增字段时，在同一迁移中写 `COMMENT ON TABLE` / `COMMENT ON COLUMN`；字段含义变更时同步更新注释。覆盖主键、外键或逻辑关联、审计字段和版本字段，按需说明枚举值、默认值、单位、可空含义及北京时间口径。注释以实际实现为准，不把逻辑关联描述成数据库外键，不记录任何真实凭据。Java 注释和 SQL 的 `--` 说明不能替代数据库元数据注释。
- 补充已执行迁移遗漏的中文说明时，新建独立的注释迁移，参考 `V7__table_and_column_comments.sql`；不修改旧迁移校验和，不重建表。交付前对照表结构逐列核对覆盖，若执行迁移，仅通过目录元数据确认注释，不查询业务记录；清楚区分迁移文件已编写与数据库中已生效。

## 领域纯单元测试

新增或修改领域行为时，补充对应纯单元测试；仅注释、格式、接口重命名不机械新增测试。测试放在所属业务模块 `src/test/java`，包路径与领域对应，类名使用 `*UnitTests`。参考 `UserUnitTests`、`PermissionUnitTests`。

- 使用现有版本管理的 JUnit Jupiter 和 AssertJ，直接构造聚合并调用公开行为。不得使用 `@SpringBootTest`、Spring 上下文、MockMvc、数据库（包括 H2）、Redis、容器或网络。只有协作者确实存在时才考虑 mock，不 mock 被测聚合。
- 覆盖创建、状态变化、正常输入、空值/空白、长度边界、非法输入及稳定业务错误码；验证不可变字段和失败操作的合理状态约束。避免仅为 getter 或内部调用顺序编写测试。
- 时间断言使用调用前后区间或已注入的可控时钟，不 sleep、不要求两次时间严格不同。ID 检查类型/格式与独立生成，不断言具体值。
- 唯一键、JPA 自动版本递增、真实事务回滚和 HTTP 鉴权属于集成测试范围，不宣称纯单元测试已验证。发现失败先判断业务契约还是测试假设有误，不为了变绿而固化缺陷；必要修复限定在任务范围并明确说明。
- 补充测试代码与执行测试分开：用户明确要求本次测试时，只运行授权范围；用户要求“不用测试”或未授权执行时，保留项目默认不运行测试的边界。本次 User/权限测试的授权不扩展到未来全部测试任务。

在 `src/java` 下，仅执行这两个领域测试的命令：

```powershell
mvn.cmd -pl dedsi-arche-system -am test '-Dtest=UserUnitTests,PermissionUnitTests' '-Dsurefire.failIfNoSpecifiedTests=false'
```

选择器必须与本次实际测试类一致；`failIfNoSpecifiedTests=false` 仅用于允许上游 common 模块无匹配测试，仍须检查 system 的报告，确认目标测试确实执行且数量非零。不运行全量 Host 测试来代替纯单元测试。仅修改测试及 test-scope 依赖不需要重启服务。

## 交付前自查

对照本次参考的 User 文件，逐项复核实际差异，而不只确认编译通过：

1. 分层、文件布局、仓储/查询职责和 DTO 转换位置是否一致？不同之处是否有需求依据？
2. 接口名及文件名是否均为 I 前缀？所有引用是否同步？
3. 新增字段与关键方法是否有有用的中文注释？方法、条件、record 和长查询是否易读？
4. 主键、校验、空值语义、分页、HTTP 状态、鉴权、版本冲突和迁移是否互相匹配？每张业务表、每个字段是否有准确的中文数据库注释？
5. 原有用户修改是否保留？API 示例和文档是否同步？是否误改了未授权的前端或其他领域？
6. 领域行为是否有对应纯单元测试？执行是否得到授权且限定范围？报告中的实际用例数、失败数、跳过数是否已核实？

按工作流程执行必要构建，默认不运行测试，也不以 lint、独立 typecheck 或接口调用代替用户测试。接口更名时注意旧 class 残留，只清理已确认属于本仓库的构建产物。是否重启遵循运行指南和用户当前要求；文档/skill 修改不构建、不重启服务。

交付说明实际改动、与 User 的必要差异、构建结果、是否重启以及未运行的测试；不能把启动成功或编译成功描述为功能测试通过。
