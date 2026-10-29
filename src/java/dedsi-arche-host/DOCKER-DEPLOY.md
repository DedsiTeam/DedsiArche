# dedsi-arche-host Docker 打包与部署

仅部署 Host 应用，PostgreSQL 和 Redis 使用已有实例。除服务器部署步骤外，本文件中的命令均从后端根目录 `src/java` 执行，使用 Host 目录下的独立 Compose 配置；`src/java/compose.yaml` 用于本地依赖，不参与本部署流程。

从仓库根目录打开终端时，先执行：

```powershell
cd src/java
```

本地直接运行应用的默认端口为 11518；本部署配置显式设置容器端口为 8080，宿主机端口由 `HOST_PORT` 控制，默认也是 8080。

## 1. 打包

构建机器需要 JDK 26、Maven 和 Docker（Linux 容器模式）。先确认 Maven 使用 JDK 26：

```powershell
mvn --version
mvn clean verify -pl dedsi-arche-host -am
```

`-am` 同时构建 Host 依赖的模块，依赖会打入 Host 的可执行 JAR。默认测试使用 H2，无需启动数据库容器。产物为：

```text
dedsi-arche-host/target/dedsi-arche-host-1.0.0-SNAPSHOT.jar
```

确实需要跳过测试时可使用 `mvn clean package -pl dedsi-arche-host -am -DskipTests`。

## 2. 准备部署配置

首次部署复制示例，已有配置不要覆盖：

```powershell
Copy-Item dedsi-arche-host/.env.example dedsi-arche-host/.env
```

编辑 `dedsi-arche-host/.env`，填写数据库、Redis 和 JWT 配置。该文件已被仓库现有 `.gitignore` 忽略，不会进入镜像。Compose 命令显式指定此文件，与后端根目录本地开发 `.env` 区分。

| 变量 | 说明 |
| --- | --- |
| `DB_URL` | 容器可以访问的 PostgreSQL JDBC URL |
| `DB_USER` / `DB_PASSWORD` | 已有数据库账号与密码 |
| `REDIS_HOST` / `REDIS_PORT` / `REDIS_PASSWORD` | 已有 Redis 地址、端口与密码 |
| `JWT_SECRET` | 至少 32 字节随机数据的 Base64 编码，重启时保持不变 |
| `HOST_PORT` | 宿主机对外端口，默认 8080 |
| `IMAGE_TAG` | 镜像标签，默认 1.0.0 |
| `DOCS_ENABLED` | 是否开启接口文档，默认 false |

示例地址适用于通过 Docker Desktop 访问宿主机上的 54329、63799 端口。部署到 Linux 服务器或远程依赖时，改为容器可访问的实际地址和端口；容器中的 `localhost` 指向容器自身。此配置不会创建数据库或 Redis。

数据库 `dedsi_arche` 需要预先存在，账号需要具备 Flyway 建表及迁移权限。应用启动时自动执行迁移，再由 Hibernate 校验结构。

在 PowerShell 中生成 JWT 密钥，将输出填入 `.env`：

```powershell
$jwtBytes = New-Object byte[] 32
$jwtRng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
$jwtRng.GetBytes($jwtBytes)
$jwtRng.Dispose()
[Convert]::ToBase64String($jwtBytes)
```

若密码含 `$` 等字符，在 `.env` 中用单引号包裹值，避免 Compose 插值，例如 `DB_PASSWORD='your$password'`。

## 3. 构建镜像并启动

```powershell
docker compose --env-file dedsi-arche-host/.env -f dedsi-arche-host/compose.yaml config --quiet
docker compose --env-file dedsi-arche-host/.env -f dedsi-arche-host/compose.yaml up -d --build
docker compose --env-file dedsi-arche-host/.env -f dedsi-arche-host/compose.yaml logs -f host
```

镜像默认名为 `dedsi-arche-host:1.0.0`，运行时使用 Java 26 JRE 和非 root 用户。使用 JRE 减少完整 JDK 开发工具带来的体积；构建 JAR 的机器仍需 JDK 26。镜像只复制可执行 JAR，不包含 `.env`、源码或 Maven。

`up -d` 成功仅表示容器已启动。继续检查日志及健康端点：

```powershell
Invoke-RestMethod http://localhost:8080/dedsi-arche-api/actuator/health
```

健康状态应为 `UP`；如修改 `HOST_PORT`，同步修改访问端口。启用 `DOCS_ENABLED=true` 后，文档地址为 `/dedsi-arche-api/scalar`。

## 4. 将镜像部署到另一台服务器

构建机器先完成打包，再构建及导出镜像：

```powershell
docker build -t dedsi-arche-host:1.0.0 dedsi-arche-host
docker save -o dedsi-arche-host-1.0.0.tar dedsi-arche-host:1.0.0
```

将镜像 tar 和 `dedsi-arche-host/compose.yaml` 传到服务器同一部署目录，并在该目录单独准备服务器的 `.env`。构建机器与服务器需使用相同的目标 CPU 架构；跨架构构建需指定目标平台并具备对应构建支持。

在服务器部署目录执行：

```bash
docker load -i dedsi-arche-host-1.0.0.tar
docker compose --env-file .env -f compose.yaml up -d --no-build
docker compose --env-file .env -f compose.yaml logs -f host
```

服务器只需要 Docker 和 Compose，无需 Maven、JDK、源码或 Dockerfile。`.env` 中的 `IMAGE_TAG` 必须与导入镜像的标签一致。

## 5. 更新与停止

本机构建更新：重新执行打包命令，再执行第 3 节的 `up -d --build`。远程更新：构建新标签、导出并导入服务器，然后更新服务器 `.env` 中的 `IMAGE_TAG`，再次执行 `up -d --no-build`。

停止并删除 Host 容器（从后端根目录 `src/java` 执行）：

```powershell
docker compose --env-file dedsi-arche-host/.env -f dedsi-arche-host/compose.yaml down
```

该 Compose 文件只管理 Host，不管理外部数据库或 Redis。
