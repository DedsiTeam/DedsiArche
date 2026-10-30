# API 与认证

## 源码与服务层

核对 `src/react/src/services/users.ts`、`http.ts`、`auth.ts`，`lib/api-error.ts`、`user-validation.ts`，以及目标领域的后端 Controller/请求 DTO/查询 DTO。下面是 User 参考，不是所有领域强制同一 HTTP 方法。

| 用途 | User 请求 | 关键数据 |
| --- | --- | --- |
| 分页 | POST /api/users/page | keyword、phone、email、enabled（全部时省略）、page 从 1、size=10 |
| 新增 | POST /api/users | username、displayName、phone、email、password |
| 编辑 | PATCH /api/users/{id} | displayName、phone、email、enabled、version |
| 详情 | GET /api/users/{id} | 返回 IManagedUser |
| 删除 | DELETE /api/users/{id}?version=… | version 是 query 参数 |
| 重置密码 | POST /api/users/{id}/reset-password | password、version |

- `IManagedUser`：id/username/displayName/createdAt 为 string，phone/email 为 string|null，enabled boolean，version number。`IUserPage`：items/total/page/size。新领域按其后端字段建接口，不假设 enabled、用户名或电话号码必然存在。
- services 使用已有 http，泛型声明响应并返回 data；页面不创建 Axios 实例，不重复拼 API 根路径。当前 http 超时 15 秒，根地址来自 VITE_API_BASE_URL；回退地址仅是现有配置事实，不复制硬编码端口到新服务。
- JSON 字段大小写、枚举值、长度、数字范围、null/空串/省略、日期、分页最大值逐项对齐后端。多选/boolean/数字的 UI 值在明确边界转换；ID 不转数字。
- API 已采用 PUT 的领域不能仅因 User 是 PATCH 就改请求方法；先读实际接口。前后端修改需同一授权范围，否则报告契约差异。
- version 来自读取结果，编辑和删除必须发送；冲突让用户刷新重读，不自动覆盖、不无条件重试非幂等写请求、不自行增加版本号。
- 只读预检查通过不保证写入成功；唯一约束、版本与鉴权由后端执行。禁止通过前端 mock 制造成功。

## 错误映射

| 情况 | 页面处理 |
| --- | --- |
| 唯一约束冲突 | 依据操作与后端业务码定位对应字段，不统一指向 username |
| 版本冲突 | 保留输入，告知资料已变化，需要重读后重试；不自动覆盖 |
| 404 | 提示记录不存在，返回列表或关闭后刷新 |
| 400 | 中文字段/表单反馈；不输出异常栈或原始服务端内部文本 |
| 401 | 沿用全局会话失效处理；登录错误另有登录语义 |
| 403 | 无权操作/页面不可访问；不解释成没有数据 |
| 429 | 操作过于频繁，稍后重试；不建立无限重试 |
| 网络/超时/5xx | 通用服务失败提示；列表可重试、表单保留输入 |

当前 `userError` 的 409 仍固定“账号已存在”，不能用于新领域所有冲突；具体差异见 known-gaps。字段错误走 Form.mapError，列表和非字段操作用 notify；不要重复发出数条相同失败消息。

## 路由、权限和会话

- `router/RequireAuth.tsx` 检查 session 并在完整用户资料缺失时统一调用 getProfile；等待期间不挂载业务页，失败可重试。未登录跳转 /login，保留 pathname+search+hash 作为 from。它不是管理员守卫。
- `LoginPage.tsx` 对 from 限制为站内绝对路径，拒绝 //、反斜线和登录循环；保留该行为，不直接信任 location.state 的跳转目标。
- 每次进入登录页（包括已登录时访问）清空当前站点全部 localStorage、当前标签页全部 sessionStorage，并调用 clearLocalSession 清除内存会话及失效旧资料请求。不再根据旧会话自动跳回系统；仅登录成功后跳转。清理按路由进入触发，不随表单输入或普通重渲染执行；两种存储独立清理，兼顾浏览器禁用存储的情况。
- User、系统、权限管理页通过 useSession 读取 canManageUsers；搜索/翻页/列表刷新只调用业务列表，不调用 /me。菜单读取同一缓存能力，服务端仍最终鉴权。
- auth.ts 使用 useSyncExternalStore，sessionStorage 保存会话；不要建立第二套 token 状态，不把 token 复制进其他存储或日志。
- 请求拦截器注入 Bearer；401 只在对应当前 token 的请求上清会话，忽略登录失败及旧会话的迟到响应。保留这种防竞态边界。
- expiresAt 是北京时间本地展示字符串；expiresAtEpochMs 与 Date.now 比较用于计时。旧格式/缺字段会话不做兼容。不要在业务日期中复用过期时间戳或添加 UTC 转换。
- 当前过期定时器直接清会话，刷新是显式操作；refreshSession 复用同一个 pending Promise，并检查结果仍属于当前刷新令牌。不要声称已有透明自动刷新或 401 重试。
- logout 等待刷新完成后向后端撤销刷新凭据，再清本地会话；失败会保留会话并提示。不因新增管理页擅自重写认证流程。
- 完整资料（id、username、displayName、boolean canManageUsers）随会话存在 sessionStorage；普通用户的 false 也是有效缓存。getProfile 默认复用缓存，缺失时合并同一 token/资料版本下的请求，失败释放 Promise 可重试。登录/令牌刷新返回的完整资料直接使用，不额外 GET /me。
- updateProfile 的 PATCH 响应直接更新缓存；管理编辑仅在修改本人时调用 refreshProfile。其他用户以及系统/权限变更不刷新当前用户。资料更新或会话更换后，旧读请求不能覆盖新缓存；登出/过期/401 清会话，同时失效旧请求。

## 登录、账户中心与其他领域的边界

- 登录不复用注册密码长度规则，只保留现有必填等校验；username trim，password 原样发送。保留 USER_DISABLED、401、403、400、429 的登录提示。
- 不恢复公开注册入口、/register 页面或已删除的辅助链接。登录布局独立，不套管理页表格/标题规则。
- 账户中心目前仍有自己的卡片布局和旧式表单；不把它当作新管理列表模板，也不因创建 skill 顺手重构。其姓名修改、延长登录、退出保留现有服务契约。
- 权限/系统等新领域：先核对真实接口、管理员策略、ULID、不可修改编码、关联选择、状态枚举和版本；服务端派生字段不通过 editable 输入发送为可信值。不要从 User 猜测这些业务规则。

## 验证边界

当前有 `src/react/tests/user-validation.test.ts`、`login-validation.test.ts`、`auth.test.ts`；API/E2E 脚本位于 scripts。存在文件不代表覆盖充分或本次执行通过。

新校验或错误映射的测试按任务和风险补充，避免测试静态 JSX/类名来代替行为验证。用户明确授权测试后，执行所需范围；API/E2E 可能写数据，先读脚本，不自动触碰共享数据库。

当前 `bun run build` 包含 tsc -b 与 Vite 构建；实际代码交付需要时使用，按工作流程区分构建和测试。skill/文档任务不运行项目构建或任何业务测试。
