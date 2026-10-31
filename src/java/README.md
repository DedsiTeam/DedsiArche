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
| DELETE | /api/systems/{id}?version=0 | 无关联权限、岗位根引用和字典时删除，204 |

新增示例：

```json
{"code":"arche","name":"基础平台","description":"系统入口","entryUrl":"https://example.com/","status":"NORMAL","sortOrder":0}
```

创建时 status 省略/null 默认 NOT_LAUNCHED，sortOrder 默认 0。
PATCH 必填 version，其他字段省略或 null 保留原值；description/entryUrl 空串清除，name 空白非法；NORMAL 状态不可清除入口。
更新示例：`{"name":"新系统名称","status":"MAINTENANCE","version":0}`。
分页示例：`{"keyword":"平台","status":"NORMAL","page":1,"size":10}`，page 默认 1（最大 1000000），size 默认 10（最大 100），返回 items/total/page/size，按 sortOrder、id 升序。
格式/业务校验错误 400，未认证 401，非管理员 403，不存在 404，重复编码、旧版本及有关联权限、岗位根引用或字典的删除返回 409。
系统写操作与权限、已登记岗位、字典及组织写入先锁系统记录；系统更名通过同步领域事件同事务更新权限、岗位根及岗位权限、字典、组织、菜单中的系统名称，并推进实际变化的聚合版本。受影响聚合的旧版本提交返回 409。系统存在权限、岗位根、字典、组织或菜单引用时禁止删除，不级联清理。
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

岗位和系统单独改名不会自动同步 UserPosition 快照；管理员重新提交 positionIds 时刷新四个字段。系统名称同步覆盖 Permission、Position、Dictionary、Organization 与 Menu 聚合，权限名称同步维护 Position 与 Menu 聚合；两者均不扩散到用户历史快照。用户资料与岗位集合的任何保存失败共同回滚；根版本冲突返回 409。

被任意用户引用的岗位删除返回 HTTP 409 和稳定业务码 POSITION_IN_USE，需先解除用户岗位关联。岗位删除在根写锁下检查引用，和用户配置的岗位读锁协调，数据库 FK RESTRICT 最终兜底；不级联删除用户。用户删除只级联其 UserPosition 子实体，保留岗位及权限。

Flyway V12 增加 sys_users.positions_revision（默认 0）及 sys_user_positions，包含用户级联外键、岗位限制删除外键、用户与岗位联合唯一约束、系统快照成对空值检查、岗位引用索引和全部中文元数据注释。不回填或推测任何用户岗位。完整结构见 [用户 SQL 快照](docs/sql/system/users.sql)，截至 V12，仅供阅读，不直接执行到现有业务库。

## 字典管理

字典使用 Dictionary 聚合根及 DictionaryItem 子实体。根字段为 id、systemCode、systemName、code、name、description、enabled、createdAt、updatedAt、version；子项字段为 id、dictionaryId、code、name、sortOrder、isDefault、enabled。所有主键均为 26 位 ULID，业务时间为北京时间本地时间。systemCode 必须是已登记系统，systemName 仅由后端读取。同一系统内字典 code 唯一；同一字典内子项 code 唯一，两者均区分大小写且创建后不可改变。

| 方法 | 路径 | 权限与返回 |
| --- | --- | --- |
| POST | /api/dictionaries | 管理员；创建字典及完整子项，201 + 完整详情 |
| POST | /api/dictionaries/page | 管理员；返回摘要 items / total / page / size，不加载子项 |
| GET | /api/dictionaries/{id} | 管理员；整取根及全部子项，包含停用项 |
| PATCH | /api/dictionaries/{id} | 管理员；按根 version 整体保存，返回更新后的完整详情 |
| DELETE | /api/dictionaries/{id}?version=0 | 管理员；整体删除字典和子项，204，保留系统目录 |
| GET | /dedsi-arche-api/api/dictionaries/options/status/arche | 已登录用户；返回所有子项的 code / name，code 与 systemCode 路径参数均必填 |
| GET | /api/dictionaries/available?systemCode=arche&code=status | 已登录用户；字典不存在或停用返回 404，成功返回根及仅启用的子项 |

创建示例：

```json
{
  "systemCode": "arche",
  "code": "status",
  "name": "业务状态",
  "description": "状态选项",
  "enabled": true,
  "items": [
    {"code": "pending", "name": "待处理", "sortOrder": 0, "isDefault": true, "enabled": true},
    {"code": "done", "name": "已完成", "sortOrder": 1, "isDefault": false, "enabled": true}
  ]
}
```

编码和名称最多 100 字符，说明最多 500 字符，排序值为非负整数。根 enabled 默认 true；创建 items 省略或 null 默认空数组，允许空字典。子项 sortOrder 默认 0、isDefault 默认 false、enabled 默认 true。isDefault 是确切 JSON 字段名。创建字典时禁止指定子项 ID；编辑时已有项必须保留 ID 与 code，新项 ID 为空。子项整体输入省略的排序、默认与启用字段采用上述默认值，而不是保持旧子项值。

PATCH 的 version 必填；name、description、enabled、items 省略或 null 均保持。description 空白清除，name 空白拒绝。items 空数组清空，非空数组代表完整替换；未提交的旧子项删除，保留项的 ID 不变。不得移除旧项后以新 ID 重用同一个 code，必须保留原项 ID。默认项最多一个且必须启用，允许没有默认项。停用整个字典保留子项配置；业务读取不返回停用字典，已启用空字典返回空 items。管理详情和业务读取均按 sortOrder、code、id 排序。

分页示例：`{"keyword":"状态","systemCode":"arche","enabled":true,"page":1,"size":10}`。keyword 忽略大小写匹配编码或名称；systemCode 空白或省略不筛选，enabled null 不筛选；page 默认 1、最大 1000000，size 默认 10、最大 100。摘要按 systemCode、code、id 稳定排序。

所有写入在原 API 事务中维护整个聚合并在响应前 flush，子项不提供独立写入口。完整读取使用单条 LEFT JOIN FETCH，业务读取只在 DTO 组装时过滤停用项，不截断持久化集合。创建、编辑及删除先取得所属系统锁；编辑在等待系统锁后刷新聚合，再检查根 version，避免无变化更新绕过过期版本检查。根或子项实际变化推进更新时间和版本，系统名称变化也参与根版本保护。

系统改名及首次登记事件在原事务中按“系统 → 权限 → 岗位 → 字典 → 组织 → 菜单”的顺序同步，字典按 ID 稳定更新 systemName。同步失败时系统和全部引用共同回滚。字典存在时系统删除返回 409，外键同时限制删除，不级联移除业务字典。

业务字段校验使用 INVALID_DICTIONARY/400；所属系统不存在使用 DICTIONARY_SYSTEM_NOT_FOUND/400；请求格式错误 400、未登录 401、管理权限不足 403、字典缺失 404、唯一键或版本冲突 409。沿用现有 ProblemDetail，不引入平行鉴权或异常体系。接口自动进入现有 system OpenAPI 分组。

Flyway V13 新建 sys_dictionaries 和 sys_dictionary_items，包含系统外键、子项级联外键、编码唯一约束、非负排序与默认项启用检查及全部中文元数据注释。最多一个默认项由聚合最终校验、系统写锁和根版本共同保护，不使用会因合法默认项切换的 SQL 顺序而误报冲突的即时部分唯一索引。完整结构见 [字典 SQL 快照](docs/sql/system/dictionaries.sql)，截至 V13，仅供阅读。领域单测源码为 DictionaryUnitTests；实际执行须由用户本次授权，不将构建或启动描述为测试通过。

字典 API 回归脚本为 `src/scripts/test-dictionary-api.py`，仅在明确授权测试及临时系统/字典创建清理后运行。先使用现有管理员正常登录，将登录响应保存到仓库外权限受限的 JSON 文件（字段包含 accessToken），不要将令牌放入命令行或提交仓库。使用当前 Aspire 本地配置及 `dedsiarche-aspire-postgres` 容器；运行示例（仓库根目录）：

```sh
python3 src/scripts/test-dictionary-api.py \
  --session-file /tmp/dedsi-dictionary-session.json \
  --report /tmp/dedsi-dictionary-api-report.json
```

脚本覆盖整存整取、子项保留身份及增删改、空数组与 null、默认项切换与取消、非法输入原子性、幂等及旧版本冲突、启用过滤、系统改名名称和版本同步。真实回滚用例仅锁定本次唯一前缀的字典，观察系统改名监听器被该行锁阻塞后，由持锁事务单独将此临时字典 version 加一，触发监听器真实乐观锁失败；断言系统和其余字典全部回滚，并明确区分该人工提交的版本增量。数据库超时仅设置在专用会话，不修改全局参数、业务结构或真实记录。

finally 使用 API 清理本次系统和字典，报告根记录与子项残留数量及清理失败明细；不创建账号，不输出认证令牌、数据库连接信息或原始错误响应。报告 passed 为 true 且 remainingRoots、remainingChildren 均为 0 才表示全部用例与清理完成。异常退出时按报告中的唯一 prefix 和记录 ID 排查，不扩大清理范围；会话文件使用完由运行者安全处理。


下拉框读取全部字典项可调用 `GET /dedsi-arche-api/api/dictionaries/options/status/arche`，响应仅包含编码与名称：

```json
[
  {"code": "pending", "name": "待处理"},
  {"code": "done", "name": "已完成"}
]
```

code、systemCode 均通过必填路径参数传入，非空白且最多 100 字符，大小写敏感，查询前去除首尾空白。字典编码仅在所属系统内唯一，通过两者联合精确定位，没有匹配返回 404。完整路径为 `/dedsi-arche-api/api/dictionaries/options/{code}/{systemCode}`；例如 `GET /dedsi-arche-api/api/dictionaries/options/status/arche`。不再支持省略系统编码或通过查询参数传入编码的旧路由。

该接口登录即可读取，返回按 sortOrder、code、id 排序的全部子项，包括停用子项及停用字典中的子项，空字典返回 `[]`；不额外返回 ID、默认项或启用状态。需要仅启用字典/启用项的业务仍使用已有 `/available` 接口。


## 组织机构

Organization 是独立节点聚合，父子通过 parentId 引用，组织树不是级联写入的大聚合。字段为 id、systemCode、systemName、parentId、code、name、name1～name4、sortOrder、description、enabled、version、createdAt、updatedAt。id 使用 ULID，时间使用北京时间；code 在系统内唯一且与 systemCode 一样创建后不可改变。parentId=null 表示顶级，备用名称不强制语言含义。

全部接口要求管理员，路径带现有 `/dedsi-arche-api` 上下文前缀：

| 方法 | 路径 | 行为 |
| --- | --- | --- |
| POST | /api/organizations | 创建独立节点，201 + 节点详情 |
| GET | /api/organizations/{id} | 节点详情，未找到 404 |
| POST | /api/organizations/page | 分页独立节点，返回 items、total、page、size |
| GET | /api/organizations/tree/{systemCode} | 完整根节点数组；每个节点包含详情字段及 children；含停用节点，无节点返回 [] |
| PATCH | /api/organizations/{id} | 修改节点及其上级，返回最新详情与 version |
| DELETE | /api/organizations/{id}?version=0 | 删除叶子节点，204；任何子节点（含停用）均阻挡删除 |

创建示例：

```json
{
  "systemCode": "arche",
  "parentId": null,
  "code": "headquarters",
  "name": "总部",
  "name1": "Headquarters",
  "sortOrder": 0,
  "enabled": true
}
```

systemCode、code、name 必填且最多 100 字符；name1～name4 可空且各最多 100 字符，description 可空且最多 500 字符。创建时 parentId 可省略或 null，sortOrder 默认 0、必须非负，enabled 默认 true，系统名称由服务端读取。

编辑 version 和 parentId 字段必须出现；parentId 允许 null（移到顶级），省略会返回 400，空白标识同样非法。其余字段省略或 null 保持原值；name1～name4、description 空白可清除。移动仅修改当前节点 parentId，子树后代关系自然保留。父节点须存在、同系统，不能是自身或后代；启用节点的直接父节点必须启用；有启用子节点时拒绝停用当前节点，不自动级联启停。

分页请求为 `{"keyword":"总部","systemCode":"arche","enabled":true,"page":1,"size":10}`；keyword 忽略大小写匹配主名称或编码，systemCode 省略/空白不筛选，enabled null 不筛选。page 默认 1、上限 1000000，size 默认 10、上限 100。分页按 systemCode、sortOrder、code、id 排序；完整树每层按 sortOrder、code、id 排序。树通过单次查询读取全部节点，不按筛选裁剪父节点。

同系统全部写入先取得系统行锁，编辑和删除在等待系统锁后刷新节点再校验 version；父链校验在锁内并记录已访问节点，避免并发互移形成环或异常环导致无限遍历。无实际变化不推进根版本，但仍校验提交版本。系统名称更名及首次登记事件在原事务中按系统、权限、岗位、字典、组织、菜单的固定顺序同步，组织按 ID 顺序更新，实际名称变化推进 version；失败共同回滚。系统存在任何组织引用时禁止删除。

业务校验使用 INVALID_ORGANIZATION/400，系统不存在使用 ORGANIZATION_SYSTEM_NOT_FOUND/400；旧版本、重复系统内编码、非叶子删除、有启用子节点的停用操作返回 409，沿用现有 ProblemDetail。V14 新建 sys_organizations，包含同系统父子复合外键、系统外键、删除 RESTRICT、编码唯一约束、非负排序/非自身父检查和全部中文数据库注释；数据库不级联删除子树。完整结构见 [组织 SQL 快照](docs/sql/system/organizations.sql)。纯领域测试源码为 OrganizationUnitTests，本次未执行测试。


### 系统名称事件覆盖范围

统一 `SystemRenamedListener` 通过 Spring 同步 `@EventListener` 和 `MANDATORY` 事务传播加入发布者事务；系统改名与首次登记复用同一同步入口。引用更新失败向上传播，使系统修改及此前已刷新的引用更新共同回滚，不使用异步或提交后监听。

| 名称字段 | 系统改名后的行为 | 并发版本归属 |
| --- | --- | --- |
| Permission.systemName | 同步当前系统名称 | Permission 根 version |
| Position.systemName | 同步已登记归属系统名称；旧 null 归属保持 | Position 根 version |
| PositionPermission.systemName | 只同步匹配系统编码的子项，完整保留其他权限 | Position 根 version；与根同时变化仅推进一次 |
| Dictionary.systemName | 同步当前系统名称，全部字典项保持 | Dictionary 根 version |
| Organization.systemName | 同步当前系统名称，上级关系及整棵树结构保持 | Organization 根 version |
| Menu.systemName | 同步系统名称，路由/树结构及权限绑定保持 | Menu 根 version |
| Menu.permissionName | 权限改名通过 PermissionRenamedEvent 同步 | Menu 根 version |
| UserPosition.systemName | 保留用户配置岗位时的历史快照；重新提交 positionIds 才刷新 | 不因系统改名推进 User 版本 |

名称未实际变化时不推进引用聚合版本。权限本身改名另通过 `PermissionRenamedEvent` 同事务更新 `PositionPermission.permissionName` 和 `Menu.permissionName`；`UserPosition.positionName` 同样属于历史快照，不自动订阅岗位改名。这些例外是明确的快照语义，不是事件遗漏。后续新增持有实时系统名称的领域，应同时接入同步协调器、系统删除引用保护，并维护上述覆盖表。


系统名称完整同步与回滚测试脚本为 `src/scripts/test-system-name-sync-api.py`。仅在明确授权接口测试及临时业务数据创建/清理后运行，使用现有管理员正常登录的仓库外受限 JSON 会话文件，不创建账号：

```sh
python3 src/scripts/test-system-name-sync-api.py \
  --session-file /tmp/dedsi-system-sync-session.json \
  --report /tmp/dedsi-system-sync-api-report.json
```

脚本创建唯一前缀的系统、权限、岗位及权限子项、字典及子项、组织父子节点；包括停用字典与组织。成功改名时逐字段核对系统及所有实时名称引用、版本恰好加一，子项 ID 及其他字段保持；同名提交确认所有引用完全不变。真实回滚用例在最后一个组织 ID 的行锁处阻塞监听器 UPDATE，确认阻塞后仅由独立事务将该临时组织 version 加一，触发真实乐观冲突 409；断言系统、权限、岗位、字典及全部组织共同回滚，唯独保留人为提交的目标组织版本增量。

finally 按组织叶子、字典/岗位、权限、系统顺序通过 API 清理，仅删除本次系统下已确认的临时记录；报告逐表根记录、岗位权限/字典子项以及专用锁会话剩余数量，全部为 0 且 passed=true 才算成功。不输出凭据及原始错误响应，不改数据库结构或全局参数。UserPosition 历史快照不在同步范围，也不为测试创建用户。会话文件用完后由运行者安全处理。


## 菜单管理

Menu 是独立节点聚合，使用 ULID 主键。字段包括 id、systemCode/systemName、parentId、code/name、menuType、permissionId/permissionCode/permissionName、routePath/componentPath、sortOrder、description、visible、enabled、cache、affix、version、createdAt/updatedAt。systemCode 和 code 创建后不可修改；code 系统内唯一。时间为北京时间。树层级由 parentId 关系派生，前端可显示根为 1 的层级，API 不持久化或返回 level。

| 方法 | 路径 | 契约 |
| --- | --- | --- |
| POST | /api/menus | 创建节点，201 + 完整节点 DTO |
| GET | /api/menus/{id} | 节点详情 |
| POST | /api/menus/page | 分页独立节点，items / total / page / size |
| GET | /api/menus/tree/{systemCode} | 全部根节点数组，节点附 children；含隐藏及停用菜单 |
| PATCH | /api/menus/{id} | 编辑独立节点并返回最新版本 |
| DELETE | /api/menus/{id}?version=0 | 仅删除叶子节点，204 |

全部接口沿用管理员鉴权及 `/dedsi-arche-api` 上下文前缀。分页条件为 `{"keyword":"用户","systemCode":"arche","menuType":"PAGE","enabled":true,"page":1,"size":10}`，类型/状态可省略，keyword 按编码/名称忽略大小写匹配，页码默认 1、最大 1000000，每页默认 10、最大 100。树按同层 sortOrder、code、id 排序，分页先按 systemCode 再按同样顺序排序；无节点返回空树。

创建示例：

```json
{
  "systemCode": "arche",
  "parentId": null,
  "code": "users",
  "name": "用户管理",
  "menuType": "PAGE",
  "permissionId": null,
  "routePath": "/users",
  "componentPath": "users/index",
  "sortOrder": 0,
  "visible": true,
  "enabled": true,
  "cache": true,
  "affix": false
}
```

systemCode、code、name 最多 100 字符，description 最多 500，sortOrder 非负默认 0。创建 menuType 必填；visible/enabled 默认 true、affix 默认 false，PAGE 的 cache 默认 true，其他类型 false。parentId、permissionId 创建可省略/null；编辑 version、parentId、permissionId 字段必须出现，parentId=null 移为顶级，permissionId=null 解除绑定。其他字段 null/省略保持，路径、组件、说明空白清除。转换类型时必须明确提交符合目标类型的路径/组件/cache/affix 组合，不自动保留无效标记。

DIRECTORY 不允许 routePath/componentPath，cache/affix 必须 false；PAGE routePath 必须以单个 `/` 起始，最长 500 字符、系统内唯一，不允许协议、查询串、片段或解码后路径穿越，componentPath 必填、最多 255 字符且为相对逻辑路径，只允许字母数字及 `_./-`，不能以 `/` 开头/结尾或包含 `//`、`.`/`..` 路径段；EXTERNAL 只接受完整 HTTP/HTTPS 地址，最长 2048 字符、无用户凭据，componentPath 为空、cache/affix=false。PAGE 500 字符上限同时保障 PostgreSQL UTF-8 联合唯一索引键长度，外链不参与页面路径唯一约束。

只有同系统 DIRECTORY 可以作为父节点；禁止自身/后代循环，有任何子节点不能改成 PAGE/EXTERNAL。启用节点要求父启用，有启用子节点拒绝停用，删除必须是叶子（含停用子节点也阻止删除）。权限可为空、所有菜单类型均可绑定同系统权限；权限编码/名称只能由服务端读取，停用权限仍可配置。此阶段仅管理配置，visible 不是访问授权，permissionId 绑定也不直接替代接口鉴权或动态加载任意组件。

写入遵循系统锁、权限读锁及刷新、菜单刷新/version 预检顺序。系统改名和权限改名分别复用原同步事务监听器，菜单按 ID 顺序在已有领域之后更新名称，实际变化推进 Menu.version；任一同步失败全部回滚。删除系统检查菜单引用，删除权限先系统锁再权限写锁，菜单仍引用时返回 409，外键 RESTRICT 兜底；历史未登记系统的权限删除仍保留原能力。

字段/类型/父链/权限归属错误为 INVALID_MENU/400，系统不存在为 MENU_SYSTEM_NOT_FOUND/400；编码/页面路径重复、旧版本、非叶子删除、含启用子节点的停用及含子节点的非目录转型返回 409。权限删除被菜单阻止时提示“权限已被菜单引用，请先解除菜单权限关联”。V15 新增 sys_menus、同系统父外键、系统及权限外键、类型组合/权限快照成组检查、PAGE 条件唯一索引、查询索引和全部中文元数据注释；见 [菜单 SQL 快照](docs/sql/system/menus.sql)。纯领域测试源码 MenuUnitTests 本轮未执行。

现有 `test-system-name-sync-api.py` 在菜单加入前编写，覆盖权限、岗位、字典、组织同步及组织处故障回滚，尚未覆盖菜单同步；不能将其历史通过结果解释为菜单已测试。本轮不扩展或执行测试脚本。

## 登录审计

LoginAudit 是独立不可变事实，id 使用 ULID，无 version、修改或删除入口。记录 loginTime（北京时间）、result（SUCCESS/FAILURE）、reasonCode（LOGIN_SUCCEEDED/BAD_CREDENTIALS/USER_DISABLED/INVALID_REQUEST/SERVICE_ERROR）、loginAccount、userId（UUID 逻辑引用，可空）、displayName 历史快照、clientIp、userAgent、failureMessage 和 traceId。用户删除不级联审计，用户改名不更新历史姓名；与系统/权限名称事件无关。

仅 `POST /api/users/login` 的实际登录处理器记录审计，不扩展到注册、令牌刷新、注销或其他接口。成功事实与刷新会话在原登录事务中共同保存并 flush，任何写入/提交失败都不返回成功令牌；afterCommit 仅标记事务已提交，不再次写库。事务提交后响应序列化或客户端断开不将已成功认证改记失败。

MVC 拦截器在参数解析前建立请求专属上下文，完成回调在事务代理之外统一记录失败，调用独立 Spring Bean 的 REQUIRES_NEW 方法并在代理外捕获写入/提交异常。错误密码、停用用户保留既有外部响应；JSON、参数校验及不支持媒体类型的 400/415 记录 INVALID_REQUEST，服务与提交失败记录 SERVICE_ERROR。失败审计不可用时仍拒绝登录，仅输出服务端 traceId 脱敏告警，不包含异常堆栈、SQL、请求或凭据。同次尝试共用审计主键，提交结果不明时主键防止成功/失败重复事实。

只从已经解析的 LoginRequest 提取账号，不缓存或重读原始请求正文，不持有密码、令牌、Cookie、完整请求头或原始异常消息。JSON 无法解析时账号为空；错误密码不绑定已存在用户身份，只有密码确认后才记录 userId/displayName。账号最多 200 字符、姓名 100、IP 64、User-Agent 512、traceId 64，均去控制字符并截断，失败文案固定映射。审计限长不改变原登录账号/密码校验规则。clientIp 取 Servlet 连接来源地址，不自行信任 X-Forwarded-For；traceId 由服务端 ULID 生成，不采信请求头。

| 方法 | 路径 | 权限与返回 |
| --- | --- | --- |
| POST | /api/login-audits/page | 管理员；items / total / page / size |
| GET | /api/login-audits/{id} | 管理员；完整只读事实 DTO；非法 ID 400，不存在 404 |

分页示例：

```json
{
  "loginAccount": "alice",
  "displayName": "管理员",
  "clientIp": "127.0.0.1",
  "result": "FAILURE",
  "reasonCode": "BAD_CREDENTIALS",
  "from": "2026-10-31T00:00:00",
  "to": "2026-11-01T00:00:00",
  "page": 1,
  "size": 10
}
```

账号/姓名包含匹配并忽略大小写，IP 精确匹配；可选 keyword 同时匹配账号或姓名。时间使用北京时间本地 ISO 格式，半开区间 `[from,to)`，允许单边省略，双边起始时间必须早于截止时间。结果和原因条件独立组合，矛盾条件返回空结果。page 默认 1、最大 1000000，size 默认 10、最大 100；按 loginTime、id 倒序。列表与详情字段一致，除 id/loginTime/result/reasonCode 外字段均允许为空。

V16 新增 sys_login_audits、受控结果/原因一致性检查、时间/结果/原因/IP 组合索引及完整中文注释，无用户外键或级联操作；见 [登录审计 SQL 快照](docs/sql/system/login_audits.sql)。LoginAuditUnitTests 覆盖受控文案、不可变事实、元数据清洗限长和成功身份要求，仅提供纯单测源码，本轮未运行测试。审计事务原子性与请求异常覆盖仍需另行授权集成验证。
