# 启动、配置与数据保护

## 两种独立入口

### Aspire 统一启动

- 仓库根目录运行 .\src\aspire\start.ps1，统一启动 PostgreSQL、Redis、Java 和 React（Bun）。
- 配置来源：src/aspire/DedsiArche.AppHost/appsettings.json，示例为 appsettings.example.json。实际配置包含密码，不能提交或完整打印。
- Parameters 保存用户名、密码、JWT 等参数值；AddParameter 在代码中声明并引用参数。
- Ports 集中定义基础设施、应用、Dashboard、OTLP 和资源服务端口。连接地址、启动参数、端口检查统一引用，不新增重复常量。
- PostgreSQL / Redis 节点保存镜像和数据目录；Backend 节点保存应用路径、管理员名单、跨域和文档开关。跨域中的 {FrontendPort} 引用前端端口。
- Aspire 不读取 Java .env；Java 子进程通过环境变量接收配置，并覆盖可选 .env 导入。
- 更新实际配置结构时同步示例和读取方；保持已有随机密码与 JWT，不因代码整理轮换凭据。

### Java 独立启动

- 仓库根目录运行 .\src\scripts\run-local.ps1，或在 IDEA 启动 Host。
- 使用 src/java/.env 和 application.yml，不依赖 Aspire appsettings.json。
- 原 run-local.ps1 使用 -DskipTests，跳过测试执行但仍编译测试源。代理需要重建且不测试时，使用 workflow.md 的 -Dmaven.test.skip=true 命令，并按原脚本方式加载 .env 后启动构建的 JAR；不要直接调用原脚本并声称测试编译也已跳过。原 Java 启动脚本的行为不因本指南自动变更。
- PostgreSQL、Redis 必须已启动。与 Aspire 访问同一数据库时，两套配置的地址和凭据需要匹配，但不得让两种后端启动方式同时占用同一端口。
- 只改某种启动方式时，不自动覆盖另一种方式的本地配置；公共基础设施变更时说明并同步明确需要的连接设置。

## 持久化

- 当前 PostgreSQL：C:\Dockers\PostgreSql\DedsiArcheDB，容器内 /var/lib/postgresql。
- 当前 Redis：C:\Dockers\Redis\DedsiArche，容器内 /data，保留 AOF。
- 上述路径以实际 appsettings.json 为准；检查所需已有数据文件后才允许启动，缺失则报告，不能初始化空目录掩盖错误。
- 未经明确授权，不删除数据目录、迁移备份或旧数据卷，不执行 docker compose down -v、docker volume prune 等清理操作。授权某一个旧副本清理不代表授权全局清理。
- 仓库已移除 Compose 配置，统一编排使用 Aspire，不恢复第二套默认依赖编排。
- 迁移须先停止写入、保留原数据及备份，再复制并核对完整性；旧副本不包含迁移后的新增写入。

## 重启顺序

1. 先确认是否需要重启及当前运行方式。仅审查/文档任务、用户要求保持停止或不重启时不启动服务；实际运行变更按已授权要求使其生效。确认本项目 AppHost、Java、前端进程身份，不按进程名批量结束。
2. Aspire 模式先停止本项目 AppHost 并等待其 Java/前端子进程退出、端口释放；Java 独立模式只停止对应 Java 进程，不能切换成 Aspire 全套启动。Persistent 数据库容器可能继续运行。
3. 仅在改挂载、密码等确有必要时替换数据库容器；先停写并等待停止完成，保留绑定目录和卷。
4. 在正确工作目录构建，构建失败先修复，不运行旧产物并宣称修改生效。
5. 按原运行方式启动新 AppHost 或独立 Java，读取启动状态与必要日志。不要并行执行停止/删除与启动。前端由 Vite 热更新即可生效的样式改动，无需重启数据库或整套 Aspire。
6. 修改端口先检查占用和 Windows 保留区间；保持用户指定的前端端口，其他服务使用配置中的固定非默认端口，不每次随机变化。

默认不做功能测试；用户明确要求的测试范围另行遵循。

## 新环境与配置变更

- 数据检查默认面向已有数据库。新机器缺少数据时，先明确是恢复备份还是用户授权的新建环境；不得悄悄创建空库。新建环境需单独设计初始化入口，保留日常启动的数据检查。
- appsettings.example.json 只能放占位值。新增配置项应同步实际配置、示例、AppHost/脚本读取方和说明；不要把包含真实凭据的整份配置加入差异输出。
- 前端可见的 VITE_* 配置不得包含数据库密码或 JWT 签名密钥。
- 修改 PostgreSQL 版本或 PGDATA 布局不能简单复用旧数据并改文件检查路径，需按该版本升级/迁移流程处理。
- CORS 明确列出允许来源；前端端口变化应同步派生来源。独立 Java 配置按共享服务的变更范围更新，不与 Aspire 配置做无条件双向覆盖。
