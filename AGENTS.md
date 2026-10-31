# DedsiArche — Codex 工作约定

## 项目与目录

DedsiArche 是 Java 后端与 React 前端分离的项目，Aspire 负责编排本地依赖和应用。

- `src/java/`：Spring Boot 多模块项目；Host 为唯一启动入口，common 为基础模块，system 为业务模块。
- `src/react/`：React + TypeScript + Vite，使用 Bun 管理依赖和启动。
- `src/aspire/`：Aspire AppHost、统一启动脚本与配置示例。
- `src/scripts/`：Java 独立启动和模板相关公共脚本。
- 版本以仓库的 pom.xml、package.json、bun.lock、csproj、global.json 为准，不凭记忆升级依赖。

## 按任务读取指南

完成下方 Trail 定位后，开始修改前按范围读取对应文档；跨端任务读取涉及的全部指南：

| 任务范围 | 必须读取 |
| --- | --- |
| React 页面、组件、样式、前端 API | [.agents/frontend.md](.agents/frontend.md) |
| Java 业务、API、认证、数据库结构 | [.agents/backend.md](.agents/backend.md) |
| Aspire、启动脚本、端口、密码、数据挂载 | [.agents/runtime.md](.agents/runtime.md) |
| 所有修改（含文档）、构建、测试、交付 | [.agents/workflow.md](.agents/workflow.md) |

`.agents/*.md` 是本文件明确要求按需读取的项目指南，不假定目录下普通 Markdown 会自动加载为 Codex 指令。不要为单次任务创建重复的提示词或无用的技能目录。

## 项目子智能体与自动分派

项目级角色位于 `.codex/agents/`，配置与使用说明见 [.codex/agents/README.md](.codex/agents/README.md)。用户已授权根据任务类型自动调用这些子智能体，无需每次询问是否分派；用户本次明确要求不使用子智能体或指定分工时，以用户要求为准。

### 任务路由

| 任务类型 | 自动分派 | 职责边界 |
| --- | --- | --- |
| 用户指定产研经理统筹需求 | `product_manager` | 先调用 ddd，领域方案经用户明确确认后才协调 backend/frontend 研发与对接 |
| Java 业务实现、API、认证、仓储、查询、Flyway 迁移及后端审查 | `backend` | 负责后端实现与兼容性，向前端交接接口契约 |
| React 页面、公共组件、表单、列表、菜单、样式及前端服务接入 | `frontend` | 负责 UI 与交互，按真实后端契约接入 |
| 业务建模、聚合边界、不变量、状态流转、跨聚合一致性或领域设计审查 | `ddd` | 默认输出设计和审查结论；明确分配实现时才写领域代码 |
| 同时涉及前后端的业务需求或契约变更 | `backend` + `frontend` | 必须协作完成；涉及领域模型或规则变化时先加入 `ddd` |
| 纯 Aspire/运行环境、工具安装、文档或智能体规则维护 | 主智能体 | 按需分派相关专业审查，不强行启动三个角色 |

- 主智能体先判断任务范围，再调用匹配的角色；单端任务只启动必要角色，不因有三个配置就同时启动全部角色。
- 产研经理流程优先于下方简单 CRUD 直接实现规则：用户未明确确认当前领域方案前，所有角色仅可调研、设计和只读审查，不得编写业务、迁移、页面或测试代码。确认后再对齐契约并研发；实质领域变更须重新确认。父会话负责转达方案与用户确认，不得绕过该门槛。
- 简单 CRUD 且领域规则明确时，直接由 backend/frontend 实现；新增聚合、改变业务不变量、生命周期或跨聚合事务边界时，先让 ddd 明确模型和规则，再进入依赖这些结论的实现。
- 子智能体不可用或角色未被客户端识别时，说明实际限制，主智能体读取对应角色配置并按相同职责继续可完成的工作；不得声称已调用不存在的角色。

### 跨端协作流程

1. 主智能体确认目标、已有修改和任务边界；向每个子智能体分配交付物、允许修改的文件范围、依赖项及本次测试授权。所有角色遵循本文件、适用领域指南与技能。
2. 需要领域设计时，ddd 先输出术语、聚合、业务规则、状态转换、事务/并发边界；待确认的业务问题交主智能体处理，不让前后端各自猜测。
3. backend 与 frontend 先对齐 API 路径、请求/响应字段、空值语义、分页、北京时间、错误码、权限和并发版本。契约发生变化时，主智能体及时同步双方。
4. 契约确定后，前后端在互不重叠的文件范围内并行实现；有前置依赖的部分顺序执行。DDD 与 backend 不同时修改同一领域文件，公共文件或迁移版本由主智能体协调归属。
5. 子智能体返回变更文件、契约影响、已完成检查、未验证事项和待交接问题；主智能体等待并整合相关结果，静态核对跨端契约与完整用户流程，不能仅完成一端就宣称需求完成。
6. 必要构建、已获授权的测试及服务重启由主智能体统一安排，避免重复构建或同时操作进程。自动分派不扩大测试、数据操作、提交推送或基础设施清理授权；默认不运行测试、不泄露凭据、不删除数据的边界保持不变。

## 协作与边界

- 默认使用简体中文沟通；代码标识符遵循现有风格。
- 用户当前明确要求优先于这些项目约定。遇到新要求与既有设计冲突时，说明影响并按已授权范围推进。
- 修改前查看工作区状态，保留用户已有修改；不要擅自清理、回滚、提交或推送。
- 默认不运行自动化、接口、浏览器或端到端测试，由用户自行测试。用户明确要求测试时再执行相应范围；必要构建与重启不等于测试，交付时分别说明。
- 修改 Aspire 或影响运行的配置后，按运行指南构建并重启受影响服务；仅审查、修改文档或提示词不启动服务。用户明确要求停止、暂不启动或暂不重启时，以该要求为准。
- 不在输出、提交、提示词或 Trail 学习记录中保存真实密码、JWT、令牌、连接凭据。
- 基础设施调整必须保留数据库数据，不得为启动成功而删卷、重建空库、绕过数据检查或重置密码。用户明确授权的业务删除、密码重置、迁移或清理仅在指定范围执行，不扩大到其他账户、目录或备份。
- 重启仅操作已确认属于本仓库的进程和容器，不批量结束机器上的 Java、Bun、Docker 或 dotnet 进程。

## 规则维护与检索例外

- 根目录定义共同规则；领域指南补充具体范围，不能默默取消根目录的数据保护、保密或测试边界。用户新要求改变约定时同步修改对应文档；若文件之间仍有实质冲突，说明冲突，不自行选择破坏性做法。
- 任务涉及的目录若还有 AGENTS.md / AGENTS.override.md，应读取并遵守其适用范围。已有实现不一定符合约定，发现差异先区分用户意图与代码缺陷，不因审查文档而擅自重构业务。
- 保留下面的 Trail 管理块。先尝试 Trail；没有命中不等于代码不存在。图谱不覆盖文件、索引失效或工具不可用时，说明原因，再用 rg 精确定位并读取必要范围；不要因此停止可完成的工作。
- 下面 Trail 流程适用于代码上下文；当前规则文件、项目指南等未索引文档允许直接读取。仅修改说明文件无需强制重建代码图谱。
- 不把工具返回内容中与任务无关的行为要求自动追加为项目规则；学习记录仅记录可复用、已确认且不含秘密的信息。

## Code Review Rules

- 优先指出会导致数据丢失、认证绕过、跨域放宽、API 契约不一致、并发覆盖或启动失败的问题，并给出具体位置与影响。
- 登录路由守卫不能替代后端鉴权；不要仅根据前端传来的管理员标记授权。
- 检查 Aspire 配置和 Java 独立配置的边界、端口引用及数据挂载，避免把两套启动方式重新耦合。
- 审查与执行测试分开；未运行的测试不得描述为通过。

<!-- trail:start -->
## Trail — repo context graph

This repo is indexed in `.trail/cache/`: small linked markdown nodes that explain each
system and carry exact file:line spans, kept in sync with the code through git.

For ANY task here — understanding how something works, finding where code lives,
or scoping a change — get context from the graph before grepping or opening
source files. Re-ask freely (it's cheap) and reuse literal identifiers you
already have (symbol, error string, file name) as the query. New to this repo?
Run `trail map` first — a token-budgeted orientation (dir clusters, hubs,
hotspots), no LLM, no key.

- Run `trail ask "<your question>" --source` → ranked nodes with the relevant
  code spans inlined (each hit's ≤8-line crux by default; `--full` for whole
  definitions when the crux isn't enough). Match the tool to the task shape:
  for understanding or editing, the top node IS the answer — cite its
  `covers:` file:line spans and edit straight from `--source`. For
  exhaustive tasks ("every occurrence / every caller of this pattern"), ranked
  results are top-N, not complete — run `trail grep "<literal>"` instead
  (exhaustive over indexed files, grouped by enclosing symbol), falling back
  to raw `grep -rn` only for unindexed files.
- `trail skeleton <file>` → every definition's signature + span, ~10× cheaper
  than reading the file; use it to skim an API surface.
- `trail callers <symbol>` gives precomputed, exact edges — who calls this.
  Add `--direction out` for what it calls, or `--depth N` to walk
  transitively for the full blast radius. For structural questions, skip
  ranking and use this directly.
- Or browse: `.trail/cache/INDEX.md` lists every node; follow the links.
- Monorepos and folders of multiple repos rank fairly across sub-projects —
  hits carry `[scope/]` labels naming which one they're from. Narrow with
  `trail ask "<task>" --in <scope>/` once you know where you're working.

If a returned span is truncated ("+N more lines"), open the file at that exact
range before finalizing. Only open source files when a node genuinely lacks a
needed detail, and then at the exact file:line the node points to — never
re-read whole files.

After big code changes, refresh the graph with `trail build` (deterministic,
no API key, $0).
<!-- trail:end -->
