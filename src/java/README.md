# DedsiArche

### 始于本原，成于至简

*Rooted in Essence. Built with Simplicity.*

---

**Arche** 源自古希腊语 ἀρχή，意为「本原」「起始」与「第一原则」，象征着对事物本质的探索。

**DedsiArche** 以简洁架构（Clean Architecture）与领域驱动设计（DDD）为核心理念，主张从业务本质出发，以清晰的领域边界组织系统，以克制的架构设计应对复杂性。

> 我们相信，**好的架构不是复杂性的堆砌，而是对本质的回归。**

不为抽象而抽象，不为架构而架构。让技术服务于领域，让简单承载复杂。

**始于本原，成于至简。**

---

## 项目概览

一个 Host + 多业务模块的 Maven Archetype 模板。初始仅包含 User 领域。

技术栈：Java 26、Spring Boot 4.1.2、Spring Data JPA/Hibernate、Spring Security JWT、PostgreSQL、Redis。
目录：dedsi-arche-common（基础类型）、dedsi-arche-system（用户业务）、dedsi-arche-host（唯一启动入口）。
领域在每层下组织：system/api/users、system/domain/users、system/infrastructure/users。
API 编排用例和转换 DTO，不设 Application、DO 或 ApiMapper。

## 运行示例

通过 [Aspire 统一启动](../aspire/README.md) 后端、PostgreSQL 和 Redis：在仓库根目录运行 `src/aspire/start.ps1`，使用 C:\Dockers 中已迁移的数据。Compose 配置已移除。

本页命令除另有说明外，均在后端根目录 `src/java` 执行。从仓库根目录打开终端时，先执行：

公共脚本位于 `src/scripts`，会自动定位 `src/java`，不依赖执行时的工作目录。模板生成的独立项目仍保留其根目录下的 `scripts`。

```powershell
cd src/java
```

安装全局 Maven（当前验证版本 3.10.0），将其 bin 目录加入 PATH，配置 JAVA_HOME 为 JDK 26。使用 `mvn --version` 确认安装成功，启动 Docker Desktop，然后执行：

```powershell
..\scripts\init-local.ps1
# 先按 Aspire 启动说明启动依赖；独立运行后端时先停止 Aspire 中的 dedsi-arche-host
..\scripts\run-local.ps1
```

另一个终端也先进入 `src/java`，再执行 `..\scripts\smoke-test.ps1`。本地默认端口为 11518；如果设置了 `SERVER_PORT`，通过 `-BaseUrl` 指定对应地址。
仅构建测试：`mvn clean verify`。
详细 API、认证语义、环境变量和模块约定见 [使用说明](docs/GENERATED-README.md)。

### IDEA 直接启动

在 IDEA 中直接打开 `src/java` 并导入其中的 `pom.xml`，选择 JDK 26，可直接运行 `DedsiArcheApplication` 主类，或选择 `DedsiArche Local` 点击 Run 或 Debug。
如果打开整个仓库，请导入 `src/java/pom.xml`，并将 Spring Boot 运行配置的工作目录设置为 `$PROJECT_DIR$/src/java`；共享配置位于 `src/java/.run`，需确认已加载。直接打开 `src/java` 时，共享配置中的 `$PROJECT_DIR$` 无需调整。
应用会从当前工作目录或其父目录加载 `.env` 中的数据库、Redis 和 JWT 密钥，支持工作目录为后端根目录 `src/java` 或 Host 模块目录，无需手工复制密码。运行配置中的同名环境变量会优先于 `.env`，请移除过期的密码配置。
启动前确保 Docker Desktop 与 Aspire 管理的依赖正在运行，并先停止 Aspire 中的 dedsi-arche-host，释放 11518 端口。
首次使用仍需执行 `../scripts/init-local.ps1`；`.env` 不提交到 Git。

Aspire 将 PostgreSQL 数据挂载到 `C:\Dockers\PostgreSql\DedsiArcheDB`，Redis 数据挂载到 `C:\Dockers\Redis\DedsiArche`。原命名卷和迁移备份保留，详见 Aspire 启动说明。

## 安装模板（类似 dotnet new install）

```powershell
..\scripts\install-template.ps1
```

将模板安装至本机 Maven 仓库，坐标 `com.dedsi:dedsi-arche:1.0.0`。
模板源在 target/archetype；由白名单复制生成，不包含 .env、target 或测试运行数据。
更新示例源码后重新执行安装脚本即可。此步骤只安装到本地，没有发布到 Maven Central。

## 创建项目（类似 dotnet new）

```powershell
..\scripts\new-project.ps1 -Name my-shop -Package com.example.myshop -OutputDirectory C:\Githubs
```

生成 my-shop-common、my-shop-system、my-shop-host。命令拒绝覆盖已有目录。
也可以使用标准 Maven 命令（需已安装全局 Maven）：

```powershell
mvn archetype:generate `
    "-DarchetypeGroupId=com.dedsi" `
    "-DarchetypeArtifactId=dedsi-arche" `
    "-DarchetypeVersion=1.0.0" `
    "-DgroupId=com.example" `
    "-DartifactId=my-shop" `
    "-Dpackage=com.example.myshop" `
    "-Dversion=0.0.1-SNAPSHOT" `
    "-DinteractiveMode=false"
```

## 添加业务模块

在生成的新项目根目录执行：

```powershell
.\scripts\add-module.ps1 -Name orders
```

父 POM 和 Host 引用自动更新。模块为空壳，按三层建立领域目录后自行添加业务。
借鉴 cap4j 的 JPA/聚合仓储思想，不依赖旧版 cap4j，不引入 Saga、事件总线或微服务基础设施。

## Scalar 接口文档

启动 Host 后访问 `http://localhost:11518/dedsi-arche-api/scalar`，OpenAPI JSON 位于 `/dedsi-arche-api/v3/api-docs`，System 分组位于 `/dedsi-arche-api/v3/api-docs/system`。
通过页面调用注册和登录接口，复制返回的 `accessToken`，填入 Scalar 的 Bearer 认证框，再调试 `/dedsi-arche-api/api/users/me`；不要重复填写 `Bearer ` 前缀。
默认开启文档，设置环境变量 `DOCS_ENABLED=false` 可同时关闭页面与 OpenAPI 端点。
采用 springdoc-openapi 3.2.0 的 WebMVC Scalar Starter，不使用 Swagger UI。
Host 定义标题和认证方案，业务模块在 Controller/DTO 上声明中文说明，并通过 GroupedOpenApi 注册分组。
新增业务模块的接口自动出现在全量文档中；需要独立分组时参考 UserOpenApiConfiguration。

## 用户联系方式

业务名称统一为“账号”（JSON 字段 username）和“姓名”（displayName），保留既有字段标识及数据库列名。
新增 phone（手机号）与 email（邮箱），均选填且不设唯一约束。手机号接受 7～15 位数字，可加 + 国际区号；邮箱最长 254 字符，按非空白的 本地部分@域名.后缀 校验。保存时去除联系方式首尾空白。
管理员新增、修改、详情和列表支持这两个字段。PATCH 省略或传 null 保留原值，空字符串清除。现有个人资料接口仍只修改姓名，不会覆盖联系方式。
Flyway V3 仅为 sys_users 增加 phone VARCHAR(16)、email VARCHAR(254) 两个可空列；旧数据保持不变。

按领域合并的完整 SQL 结构快照（各文件标注自己的截止版本）：[用户、用户岗位及刷新会话](docs/sql/system/users.sql)、[权限](docs/sql/system/permissions.sql)、[系统管理](docs/sql/system/managed_systems.sql)。每份包含最终字段、约束、索引和中文数据库注释，业务时间字段直接使用北京时间本地类型。快照仅供阅读参考，不由应用自动执行；新环境和现有环境均继续使用原 Flyway 迁移链。不要将快照直接执行到已有业务库，或复制到 Flyway 扫描目录。


## 权限管理

### 领域纯单元测试

User 与 Permission 的纯单元测试位于 `dedsi-arche-system/src/test/java/com/dedsi/arche/system/domain/`，类名分别为 `UserUnitTests`、`PermissionUnitTests`。直接调用聚合行为，不加载 Spring，不使用数据库、Redis 或网络。
在本目录执行 `mvn.cmd -pl dedsi-arche-system -am test "-Dtest=UserUnitTests,PermissionUnitTests" "-Dsurefire.failIfNoSpecifiedTests=false"` 可仅运行这两组测试，不运行 Host 集成测试。
覆盖领域创建、更新、校验边界、状态变化与不可变字段；数据库唯一约束、JPA 乐观锁、HTTP 鉴权不属于此测试范围。测试报告位于 system 模块的 `target/surefire-reports/`。

### 权限接口

全系统业务时间统一北京时间（Asia/Shanghai）：Java 使用 LocalDateTime，数据库使用 timestamp without time zone，JSON 为不带 Z 或偏移的 ISO 本地字符串，允许秒后小数；前端直接展示，不做时区转换或旧 UTC 格式兼容。V5 统一现有时间列类型，不增加历史回填字段。
登录/刷新响应的 expiresAt 为北京时间展示值，新增 expiresAtEpochMs 用于认证过期计时，前端不解析展示字符串来计算有效期。JWT 内部 iat/exp 仍遵循标准 NumericDate，不改变令牌有效期。缺少新计时字段的旧浏览器会话需重新登录。

权限定义位于 system 模块的 domain.permissions，包含 id、systemCode、systemName、code、name、description、enabled、sortOrder、createdAt、updatedAt、version。
主键使用 ULID（大写 26 字符字符串），由 ulid-creator 5.2.4 在服务端生成；User 继续使用 UUID。
Flyway V4 新建 sys_permissions，不修改已有用户数据。systemCode + code 联合唯一，区分大小写，保存时去除首尾空白，创建后不可修改。
系统编码、系统名称、权限编码、权限名称最多 100 字符；说明最多 500 字符；排序值为非负整数。
systemName 由系统目录读取，权限新增和更新不再采用客户端提交的系统名称；系统更名会同步权限中的系统名称并递增受影响权限版本。已有无系统目录的权限保留，需登记相同 code 的系统后才能编辑。系统引用通过服务端统一行锁和删除检查维护，不添加跳过旧数据验证的外键。

接口均要求 Bearer JWT 和现有配置中的管理员身份，路径需加部署的 context-path（默认 /dedsi-arche-api）。

| 方法 | 路径 | 用途 |
| --- | --- | --- |
| POST | /api/permissions | 新增，返回 201 和权限详情 |
| GET | /api/permissions/{id} | 详情 |
| POST | /api/permissions/page | 分页查询 |
| PUT | /api/permissions/{id} | 完整更新可编辑字段，包含启停 |
| DELETE | /api/permissions/{id}?version=0 | 按版本删除，成功返回 204 |

新增请求示例：

```json
{"systemCode":"arche","systemName":"基础平台","code":"system:user:read","name":"查看用户","description":"查看用户列表与详情","enabled":true,"sortOrder":0}
```

enabled 省略或为 null 时默认 true，sortOrder 省略或为 null 时默认 0。
更新提交 systemName、name、description、enabled、sortOrder、version；description 为 null、空串或省略时清除，其他可编辑字段必填。
详情和列表返回全部字段；createdAt、updatedAt 为北京时间本地值（无 Z、无时区偏移），version 用于更新和删除的并发检查。
分页请求示例：`{"keyword":"用户","systemCode":"arche","enabled":true,"page":1,"size":10}`。
keyword 搜索权限编码、权限名称和系统名称；systemCode 精确筛选，enabled 可省略。page 默认 1（最大 1000000），size 默认 10（最大 100）。
响应结构为 items、total、page、size；按系统编码、排序值、ID 升序排列。
参数错误返回 400，未认证 401，无管理权限 403，不存在 404，重复编码或版本冲突 409，沿用 ProblemDetail 错误响应。
接口自动纳入现有 system OpenAPI 分组。本版只管理权限定义，尚不包含角色、授权关系或基于权限码的访问判定；停用仅保存定义状态。

用户列表 POST `/api/users/page` 新增可选筛选参数 `phone`（最多 16 字符）和 `email`（最多 254 字符），自动去除首尾空白。支持普通文本部分匹配，邮箱忽略大小写；各筛选条件以 AND 组合，空条件不限制结果。列表和总数使用相同条件。搜索回到第一页，重置清空关键词、手机号、邮箱及状态并重新加载。


## 系统管理（ManagedSystem）

系统实体位于 `domain.systems`，主键为 26 位 ULID，时间为北京时间 LocalDateTime。
字段：id、code、name、description、entryUrl、status、sortOrder、createdAt、updatedAt、version。
code 去除首尾空白后全局唯一、区分大小写，创建后不可修改；code/name 最多 100 字符，description 最多 512 字符，entryUrl 最多 2048 字符，sortOrder 非负。
状态按字符串保存：NOT_LAUNCHED（未上线）、NORMAL（正常）、DISABLED（停用）、UPDATING（更新中）、MAINTENANCE（维护中）、RETIRED（退役）。状态是人工业务标记，不触发用户禁用或令牌撤销。
入口可为空，NORMAL 状态必须有有效 HTTP/HTTPS 绝对地址，禁止地址包含账号密码，允许尾斜杠；本接口不请求该地址。

全部接口复用管理员 Bearer 鉴权，并加部署 context-path：

| 方法 | 路径 | 用途 |
| --- | --- | --- |
| POST | /api/systems | 创建，201 返回详情 |
| GET | /api/systems/{id} | 详情 |
| POST | /api/systems/page | 按编码/名称关键词及状态分页 |
| PATCH | /api/systems/{id} | 编辑资料、入口、状态和排序 |
| DELETE | /api/systems/{id}?version=0 | 无关联权限和岗位根引用时删除，204 |

新增示例：

```json
{"code":"arche","name":"基础平台","description":"系统入口","entryUrl":"https://example.com/","status":"NORMAL","sortOrder":0}
```

创建时 status 省略/null 默认 NOT_LAUNCHED，sortOrder 默认 0。
PATCH 必填 version，其他字段省略或 null 保留原值；description/entryUrl 空串清除，name 空白非法；NORMAL 状态不可清除入口。
更新示例：`{"name":"新系统名称","status":"MAINTENANCE","version":0}`。
分页示例：`{"keyword":"平台","status":"NORMAL","page":1,"size":10}`，page 默认 1（最大 1000000），size 默认 10（最大 100），返回 items/total/page/size，按 sortOrder、id 升序。
格式/业务校验错误 400，未认证 401，非管理员 403，不存在 404，重复编码、旧版本及有关联权限或岗位根引用的删除返回 409。
系统写操作与权限及已登记岗位写入先锁系统记录；系统更名通过同步领域事件同事务更新权限、岗位根及岗位权限中的系统名称，并推进实际变化的聚合版本。权限及岗位旧版本提交返回 409。系统存在权限或岗位根引用时禁止删除，不级联清理。
V6 仅新增系统表；不自动创建系统、不删除已有权限。登记已有权限使用的 code 时同步其名称。
接口自动进入既有 system OpenAPI 分组。纯单元测试源文件为 `ManagedSystemUnitTests`，经用户授权后可在本目录执行：
`mvn.cmd -pl dedsi-arche-system -am test "-Dtest=ManagedSystemUnitTests" "-Dsurefire.failIfNoSpecifiedTests=false"`。

## 默认账号

Flyway `V8__default_local_user.sql` 在新环境首次启动时自动创建已启用的 `dedsi_user`，使用约定的初始密码。迁移只保存 BCrypt 哈希（工作因子 12），不保存明文密码。同名账号已存在时跳过，不重置密码、不修改资料或启用状态。此版本迁移只执行一次，日后删除账号不会在每次启动时重新创建。

Aspire 模板已将 `dedsi_user` 放入 `Backend:AdminUsernames`；Java 独立启动时通过 `USER_ADMIN_USERNAMES=dedsi_user` 指定管理员。创建账号本身不绕过后端管理员名单校验。

## 岗位管理

岗位聚合位于 `domain.positions`，包括 id、systemCode、systemName、code、name、description、enabled、sortOrder、createdAt、updatedAt、version 和权限子实体集合。岗位与子实体主键均为 26 位 ULID；岗位编码区分大小写、全局唯一，创建后不可修改。编码和名称最多 100 字符，说明最多 500 字符，排序值非负；业务时间为北京时间本地时间。

权限子实体包含 id、positionId、permissionId、systemCode、systemName、permissionCode、permissionName。配置权限时客户端只提交 permissionIds，后端读取已有权限定义并保存系统与权限信息，不采信客户端名称；系统及权限名称由相应改名事件同事务维护。同一岗位不能重复关联同一权限，不同系统的同名权限编码可同时关联。允许关联停用的权限定义；本版仅维护岗位配置，不接入用户岗位分配、登录权限或权限码鉴权。

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | /api/positions | 创建岗位，返回 201 和详情 |
| GET | /api/positions/{id} | 查询岗位详情，包含权限快照 |
| POST | /api/positions/page | 查询岗位分页，不加载权限集合 |
| PATCH | /api/positions/{id} | 部分更新，必须提交 version |
| DELETE | /api/positions/{id}?version=0 | 未被用户引用时删除岗位及关联，返回 204；引用中返回 POSITION_IN_USE/409 |

所有接口复用 `userAdministration.canManage(authentication)` 服务端鉴权，自动纳入现有 system OpenAPI 分组。未认证 401、非管理员 403、岗位不存在 404、格式或业务校验失败 400、唯一键或版本冲突 409；响应沿用 ProblemDetail。业务错误码包括 INVALID_POSITION、DUPLICATE_POSITION_PERMISSION、POSITION_PERMISSION_NOT_FOUND。

创建示例（permissionIds 中填写现有权限 ID，也可传空数组）：

```json
{
  "systemCode": "arche",
  "code": "manager",
  "name": "经理",
  "description": "负责日常业务管理",
  "enabled": true,
  "sortOrder": 0,
  "permissionIds": []
}
```

创建时 enabled 省略或 null 默认 true，sortOrder 默认 0，permissionIds 默认空集合。PATCH 的 version 必填，其他字段省略或 null 均保持原值；description 传空白清除，name 传空白拒绝。permissionIds 传空数组清空，非空数组全量替换权限集合，同时刷新保留关联的快照并保留其子实体 ID。权限名称实际变化时，通过 Spring 原生同步领域事件，在同一事务内更新全部岗位引用的 permissionName 并推进岗位 updatedAt 和 version；同步失败时权限改名一并回滚。系统名称实际变化也通过同步领域事件更新 Permission.systemName、岗位根 systemName 及匹配的岗位权限子实体 systemName；根和子项同时匹配时岗位版本仅推进一次。

```json
{"version": 0, "name": "业务经理", "permissionIds": []}
```

分页请求：`{"keyword":"经理","enabled":true,"page":1,"size":10}`。keyword 匹配编码或名称，忽略大小写；enabled 可省略。page 默认 1、最大 1000000；size 默认 10、最大 100。结果为 items / total / page / size，按 sortOrder、id 稳定排序，权限集合由详情接口查询。

Flyway V9 新增 sys_positions 和 sys_position_permissions，不改写已有迁移。子实体通过外键关联岗位及权限定义；有关联的权限删除返回 409，须先移除岗位关联，不级联删除岗位。唯一约束防止重复编码和重复关联。岗位版本保护基本信息和权限集合，写操作在响应前 flush。完整结构及逐列中文注释见 [岗位 SQL 快照](docs/sql/system/positions.sql)（截至 V11，仅供阅读，不能直接执行到现有数据库）。

纯单元测试代码为 PositionUnitTests，验证领域输入、快照更新、集合替换与失败状态；不验证数据库唯一约束、JPA 版本递增或 HTTP 鉴权。默认不执行测试。

岗位仓储通过单条 `LEFT JOIN FETCH` 查询加载根实体及权限集合，详情、修改与删除共用此入口，避免并发更新时根字段和权限集合来自不同数据库快照。

显式授权后可运行 `src/scripts/test-position-api.py --session-file <本机登录会话JSON路径> --report <报告路径>`。脚本使用正常登录产生的 accessToken，不创建账号、不记录凭据；创建唯一编码的系统、权限和岗位，并在结束时清理本次数据。覆盖整存整取、快照刷新、权限清空、并发版本冲突、受控数据库行锁下的事务回滚和整体删除。数据库锁仅作用于本次测试岗位，自动释放；脚本不默认运行，也不接入启动流程。

权限领域的 `update` 在完成全部校验后，仅针对规范化名称实际变化返回不可变 `PermissionRenamedEvent`；创建及同名更新不产生改名事件。权限 API 在已有事务中显式发布，普通同步 `@EventListener` 以 `MANDATORY` 加入事务，通过岗位领域行为修改引用名称；不使用异步、提交后监听、独立事务或消息中间件。系统名称由独立的 SystemRenamedEvent 协调，不产生 PermissionRenamedEvent。

权限更名按系统行锁、权限写锁、岗位写入的顺序执行；岗位创建及已登记岗位编辑先锁所属系统，然后按权限 ID 升序获取读锁并刷新所选权限记录，锁持续到事务结束，避免并发新增关联漏掉更名。监听器按岗位 ID 稳定顺序完整加载全部子实体，实际名称变化才推进根修改时间；重复同名处理或无引用不推进岗位版本。岗位的旧编辑请求会因同步产生的版本变化返回 409，需重新读取。Flyway V10 只更新表与字段中文说明，不修改 V9，不自动回填此前的历史名称快照。

### 岗位所属系统与系统名称同步

创建岗位必须提交已登记的 `systemCode`（最多 100 字符），`systemName` 完全由服务端读取。岗位根所属系统不限制其权限来自其他系统。详情和分页都返回根 `systemCode/systemName`；旧岗位迁移后两者仍为 null，不根据权限猜测归属。PATCH 省略或 null 保持；旧 null 可首次绑定，已绑定只接受相同编码，空白、改值或解绑均拒绝。错误码包括 POSITION_SYSTEM_NOT_FOUND、POSITION_SYSTEM_IMMUTABLE；版本冲突仍返回 409。

ManagedSystem 仅在名称实际变化时返回纯领域 `SystemRenamedEvent`，API 在原事务显式发布。单一同步监听器以 MANDATORY 加入事务，按“系统锁 → 权限 ID 顺序写锁并刷新 → 岗位 ID 顺序完整聚合更新”协调；Permission 仅更新 systemName，不连带发布权限改名事件。岗位查询通过根系统条件或子项 EXISTS 条件筛选并 fetch 全部权限，避免截断集合。首次登记系统发布 SystemRegisteredEvent，复用相同协调逻辑，继续修正已有同编码权限与岗位子项名称，但不为历史岗位根自动绑定系统。

系统改名只更新匹配的系统名称，保留权限名称、系统编码、岗位归属及其他资料；没有实际变化不推进被引用聚合版本。任一同步失败，系统本身及全部引用共同回滚。并发岗位创建、首次绑定及已有岗位编辑先锁系统再读取所选权限，避免写入过期名称。系统删除检查权限和岗位根引用，外键兜底并发删除；不删除关联业务数据。

Flyway V11 为岗位根新增可空 system_code/system_name、成对空值检查、到 sys_systems(code) 的外键和查询索引，并更新中文注释；不修改已执行的 V1—V10、不回填历史岗位归属。SQL 快照折叠至 V11。新增测试代码覆盖系统事件、失败无事件、首次绑定与不可变归属、根及子项联合同步、跨系统引用及幂等；构建和测试执行分别由本次授权决定。

## 用户岗位子实体与整聚合保存

User 仍使用 UUID 主键，新增其私有的一组 UserPosition 子实体。每项包含 id（26 位 ULID）、userId（UUID）、positionId（26 位 ULID）以及 positionCode、positionName、systemCode、systemName 四个配置时快照。岗位仍是独立聚合，用户仅持有关联与快照，不包含岗位权限子实体。历史岗位所属系统未登记时，两个系统快照均为 null；不根据权限推测归属。

管理员新增请求使用独立 CreateManagedUserRequest，支持可选 positionIds；省略或 null 默认为空。公开 `/api/users/register` 继续使用原 RegisterUserRequest，不能借此分配岗位。管理员 PATCH 保留姓名 displayName、enabled、version 必填契约，positionIds 省略/null 保持、空数组清空、非空列表全量替换并刷新快照；同一岗位不能重复，缺失或非法岗位返回 400。允许维护停用岗位关联，岗位配置不自动改变管理员身份、登录权限或后端权限码判定。

| 方法 | 路径 | 用户岗位契约 |
| --- | --- | --- |
| POST | /api/users | 管理员创建用户及岗位子实体；返回完整详情 |
| GET | /api/users/{id} | 整取用户资料及 positions 完整快照集合 |
| PATCH | /api/users/{id} | 同事务更新资料与岗位集合；返回持久化后的根 version |
| POST | /api/users/page | 保留现有摘要 items / total / page / size，不加载 positions |
| DELETE | /api/users/{id}?version=0 | 保留当前账户及管理员保护；删除用户、刷新会话和关联，不删除岗位或权限定义 |

编辑示例（positionIds 填写真实岗位 ULID）：

```json
{
  "displayName": "业务人员",
  "enabled": true,
  "version": 0,
  "positionIds": ["01ARZ3NDEKTSV4RRFFQ69G5FAV"]
}
```

新增、详情、修改响应仍包含原来的 id、username、displayName、enabled、createdAt、version、phone、email，额外增加 positions 数组。数组每项包含上述七字段，不返回密码摘要、credentialVersion 或内部 positionsRevision。现有未使用 positionIds 的管理调用继续保持岗位关联不变。

后端按岗位 ID 升序锁定并刷新岗位根，再生成 UserPositionSelection 快照并交给 User.replacePositions。保留关联的子实体 ID 不变，删除项通过 orphanRemoval 删除；新增、删除或快照实际变化推进内部 positions_revision，由 JPA 更新根 version；完全相同的集合和快照不推进岗位修订号。该内部字段不修改 credential_version，不因岗位配置撤销登录凭据。管理仓储通过单条 LEFT JOIN FETCH 完整加载用户及其岗位，认证 findById/findByUsername 保留轻量查询，DTO 转换在原 API 编排层完成。

岗位和系统单独改名不会自动同步 UserPosition 快照；管理员重新提交 positionIds 时刷新四个字段。已有系统/权限名称同步仍只维护 Permission 与 Position 聚合，不扩散到用户。用户资料与岗位集合的任何保存失败共同回滚；根版本冲突返回 409。

被任意用户引用的岗位删除返回 HTTP 409 和稳定业务码 POSITION_IN_USE，需先解除用户岗位关联。岗位删除在根写锁下检查引用，和用户配置的岗位读锁协调，数据库 FK RESTRICT 最终兜底；不级联删除用户。用户删除只级联其 UserPosition 子实体，保留岗位及权限。

Flyway V12 增加 sys_users.positions_revision（默认 0）及 sys_user_positions，包含用户级联外键、岗位限制删除外键、用户与岗位联合唯一约束、系统快照成对空值检查、岗位引用索引和全部中文元数据注释。不回填或推测任何用户岗位。完整结构见 [用户 SQL 快照](docs/sql/system/users.sql)，截至 V12，仅供阅读，不直接执行到现有业务库。
