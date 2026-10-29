# Codex 项目指南

入口是仓库根目录的 [AGENTS.md](../AGENTS.md)，其中保留 Trail 的检索规则，并明确指定按任务读取这里的文档。

- [frontend.md](frontend.md)：技术栈、界面规范、交互与 API 接入。
- [backend.md](backend.md)：模块职责、接口契约、认证与数据变更。
- [runtime.md](runtime.md)：Aspire / Java 两种启动方式、配置、数据与重启。
- [workflow.md](workflow.md)：修改流程、构建与测试边界、交付要求。

维护原则：只记录稳定、可执行的项目约定；版本与实际配置以代码和配置文件为准。新的用户决定改变既有约定时，同步更新对应文档，不复制多份规则。不要写入密码、机器进程号、临时 Dashboard 登录链接或单次调试日志。

本目录当前是项目指南，不是自定义 skills。日后确有可复用的独立流程，再按需要新增技能。

Codex 的 AGENTS.md 发现与作用域规则参考 [官方文档](https://learn.chatgpt.com/docs/agent-configuration/agents-md)。根目录规则由 Codex 加载，这里的文档由根目录规则要求按需读取。修改后建议开启新会话，以加载更新后的入口指令。

## 规则归属

根目录维护协作边界、保护原则和指南入口；workflow.md 统一定义构建/测试与交付边界；runtime.md 定义两种启动方式和重启步骤；frontend.md、backend.md 只补充领域规则。不要在多个文件重复完整流程。

示例端口、路径及工具版本属于可变事实，以项目实际配置为准；用户确认的 UI 规范和工作方式属于意图，不应仅因代码暂时不同就改写规则。文档审查发现实现差异时报告差异，不在仅完善规则的任务中自动修改业务代码。
