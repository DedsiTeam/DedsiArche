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

按领域合并的完整 SQL 结构快照（截至 V7）：[用户及刷新会话](docs/sql/system/users.sql)、[权限](docs/sql/system/permissions.sql)、[系统管理](docs/sql/system/managed_systems.sql)。每份包含最终字段、约束、索引和中文数据库注释，业务时间字段直接使用北京时间本地类型。快照仅供阅读参考，不由应用自动执行；新环境和现有环境均继续使用原 Flyway 迁移链。不要将快照直接执行到已有业务库，或复制到 Flyway 扫描目录。


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
systemName 由系统目录读取，权限新增和更新不再采用客户端提交的系统名称；系统更名会同步权限名称并递增权限版本。已有无系统目录的权限保留，需登记相同 code 的系统后才能编辑。系统引用通过服务端统一行锁和删除检查维护，不添加跳过旧数据验证的外键。

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
| DELETE | /api/systems/{id}?version=0 | 无关联权限时删除，204 |

新增示例：

```json
{"code":"arche","name":"基础平台","description":"系统入口","entryUrl":"https://example.com/","status":"NORMAL","sortOrder":0}
```

创建时 status 省略/null 默认 NOT_LAUNCHED，sortOrder 默认 0。
PATCH 必填 version，其他字段省略或 null 保留原值；description/entryUrl 空串清除，name 空白非法；NORMAL 状态不可清除入口。
更新示例：`{"name":"新系统名称","status":"MAINTENANCE","version":0}`。
分页示例：`{"keyword":"平台","status":"NORMAL","page":1,"size":10}`，page 默认 1（最大 1000000），size 默认 10（最大 100），返回 items/total/page/size，按 sortOrder、id 升序。
格式/业务校验错误 400，未认证 401，非管理员 403，不存在 404，重复编码、旧版本及有关联权限的删除返回 409。
系统写操作与权限创建/编辑统一锁系统记录；系统更名同事务更新权限名称、更新时间和版本，权限旧版本提交返回 409。删除只检查和拒绝，不级联清理权限。
V6 仅新增系统表；不自动创建系统、不删除已有权限。登记已有权限使用的 code 时同步其名称。
接口自动进入既有 system OpenAPI 分组。纯单元测试源文件为 `ManagedSystemUnitTests`，经用户授权后可在本目录执行：
`mvn.cmd -pl dedsi-arche-system -am test "-Dtest=ManagedSystemUnitTests" "-Dsurefire.failIfNoSpecifiedTests=false"`。
