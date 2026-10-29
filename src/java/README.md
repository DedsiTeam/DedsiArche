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

通过 [Aspire 统一启动](../aspire/README.md) 后端、PostgreSQL 和 Redis：在仓库根目录运行 `src/aspire/start.ps1`，复用现有数据卷。Compose 配置已移除。

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

Aspire 复用 `dedsiarche_postgres-data` 和 `dedsiarche_redis-data`。首次切换会停止旧依赖容器，但保留原容器和数据卷。

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
