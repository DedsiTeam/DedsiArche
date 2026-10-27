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

配置 JAVA_HOME 为 JDK 26，启动 Docker Desktop，然后执行：

```powershell
.\scripts\init-local.ps1
docker compose up -d --wait
.\scripts\run-local.ps1
```

另一个终端执行 `.\scripts\smoke-test.ps1`。
仅构建测试：`.\mvnw.cmd clean verify`。
详细 API、认证语义、环境变量和模块约定见 [使用说明](docs/GENERATED-README.md)。

## 安装模板（类似 dotnet new install）

```powershell
.\scripts\install-template.ps1
```

将模板安装至本机 Maven 仓库，坐标 `com.dedsi:dedsi-arche:1.0.0`。
模板源在 target/archetype；由白名单复制生成，不包含 .env、target 或测试运行数据。
更新示例源码后重新执行安装脚本即可。此步骤只安装到本地，没有发布到 Maven Central。

## 创建项目（类似 dotnet new）

```powershell
.\scripts\new-project.ps1 -Name my-shop -Package com.example.myshop -OutputDirectory C:\Githubs
```

生成 my-shop-common、my-shop-system、my-shop-host。命令拒绝覆盖已有目录。
也可以使用标准 Maven 命令（需已安装 Maven，或使用本模板 mvnw.cmd 的完整路径）：

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

启动 Host 后访问 `http://localhost:8080/dedsi-arche-api/scalar`，OpenAPI JSON 位于 `/dedsi-arche-api/v3/api-docs`，System 分组位于 `/dedsi-arche-api/v3/api-docs/system`。
通过页面调用注册和登录接口，复制返回的 `accessToken`，填入 Scalar 的 Bearer 认证框，再调试 `/dedsi-arche-api/api/users/me`；不要重复填写 `Bearer ` 前缀。
默认开启文档，设置环境变量 `DOCS_ENABLED=false` 可同时关闭页面与 OpenAPI 端点。
采用 springdoc-openapi 3.2.0 的 WebMVC Scalar Starter，不使用 Swagger UI。
Host 定义标题和认证方案，业务模块在 Controller/DTO 上声明中文说明，并通过 GroupedOpenApi 注册分组。
新增业务模块的接口自动出现在全量文档中；需要独立分组时参考 UserOpenApiConfiguration。
