# Host Docker 打包与部署

仓库已移除 Compose 配置。本地开发通过 [Aspire](../../aspire/README.md) 统一启动；本文件仅说明使用保留的 Dockerfile 构建和运行 Host 镜像，数据库与 Redis 使用外部实例。

以下命令从后端根目录 src/java 执行。

## 打包镜像

构建机器需要 JDK 26、Maven 和 Docker（Linux 容器模式）。

```powershell
mvn package -pl dedsi-arche-host -am "-Dmaven.test.skip=true"
docker build -t dedsi-arche-host:1.0.0 dedsi-arche-host
```

## 配置和运行

首次部署可复制 dedsi-arche-host/.env.example 为 dedsi-arche-host/.env；不要覆盖已有配置。填写 DB_URL、DB_USER、DB_PASSWORD、REDIS_HOST、REDIS_PORT、REDIS_PASSWORD、JWT_SECRET。数据库地址必须能从容器内部访问，localhost 指向容器自身。

docker --env-file 直接读取 KEY=value，不执行 Compose 插值；值不要加外围引号，密码中的 $ 保持原样。此文件不得提交 Git。

```powershell
docker run -d --name dedsi-arche-host --restart unless-stopped --env-file dedsi-arche-host/.env -e SERVER_PORT=8080 -e DOCS_ENABLED=false -p 8080:8080 --log-opt max-size=10m --log-opt max-file=3 dedsi-arche-host:1.0.0
docker logs -f dedsi-arche-host
```

接口前缀为 /dedsi-arche-api。通过 CORS_ALLOWED_ORIGINS 配置明确的前端来源；USER_ADMIN_USERNAMES 仅填写已存在且获授权的管理员用户名。

## 传输与停止

```powershell
docker save -o dedsi-arche-host.tar dedsi-arche-host:1.0.0
```

服务器上使用 docker load -i dedsi-arche-host.tar 导入镜像，准备服务器自己的环境文件后执行上面的 docker run。构建与运行环境需匹配 CPU 架构。

```powershell
docker stop dedsi-arche-host
```

以上操作只管理 Host，不管理或删除外部数据库、Redis。
