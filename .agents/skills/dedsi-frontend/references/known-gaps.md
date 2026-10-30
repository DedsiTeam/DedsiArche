# 当前 User 前端的差异与复用注意

以下为 2026-10-30 源码静态审阅结果，未运行浏览器或业务测试。这里只记录，不代表问题已修复；后续任务若触及这些代码，重新核对，并在修复后更新本清单。不是所有事项都需要用户决策，不得以此阻塞无关任务。

| 项目 | 证据 | 影响与后续处理 |
| --- | --- | --- |
| 409 的通用提示不准确 | `src/react/src/lib/api-error.ts:6` | 所有 409 都映射“账号已存在”。User 编辑/删除局部覆盖了提示，但新领域直接复用会误报。按业务码与操作区分重复、版本、关联约束冲突；不要把现状固化为规范。 |
| 重置密码表单未统一 | `src/react/src/components/UserActionDialog.tsx:32`、`:57`、`:60` | 仍为普通 form + state、提交时校验；错误文字缺 text-error，规则提示仍 muted。UserFormDialog 已是 RHF/Form，不能宣称所有用户弹窗都已统一。后续重构时保留密码和本人会话行为。 |
| 失焦异步校验未按指南锁定 | `src/react/src/components/ui/form.tsx:95`、`:114`；`src/react/src/components/UserFormDialog.tsx:36`、`:65` | 公共 Form 默认只根据 isSubmitting 禁用；UserFormDialog 已同时依据 isValidating 锁定字段与关闭，其他调用方仍需核对。提交前异步 validate 已在提交状态保护中，但单独失焦校验不具备同样保护。新接入异步校验需补齐，不可仅凭文档认定可用。 |
| 自动回退页码时 loading 提前结束 | `src/react/src/pages/UsersPage.tsx:41`、`:47` | 首次响应发现页码越界后 setFilters，finally 立即 setLoading(false)；新 effect 开始又未设 true。回退页请求中可能显示旧行并允许操作。将 loading 覆盖整个请求/回退周期，保留旧响应隔离。 |
| 自定义只读控件与错误定位的边界 | `src/react/src/components/ui/form.tsx:94`、`:100`、`:130`、`:138` | render 的 controlProps 没有 readOnly，需要业务自行透传；错误定位仅排除 disabled，未排除 readOnly。只读 Select 实际禁用时可能无法聚焦，不能宣称所有“不可操作字段”已转表单级错误。 |
| 旧表单样式、排版未全面一致 | `src/react/src/pages/HomePage.tsx:66`、`src/react/src/services/users.ts:2`、`src/react/src/pages/UsersPage.tsx:52` | 账户中心字段错误仍缺 error 色；services/users.ts 已按多行整理，部分旧 JSX 仍压成单行。新代码使用清晰多行及必要中文注释，不能复制后称其符合最新约定；不无授权做全仓重排。 |

另有两处现状需要区分“项目约定”和“具体页面选择”：

- 表单按钮：新增/编辑左对齐，删除/重置右对齐；当前不能宣称有统一的底部按钮顺序和对齐规范。若用户要求全站统一，再确认采用哪种布局。
- 账户中心使用 max-width 居中、图标和副标题，而管理列表明确满宽、简洁标题。按用途区分，不为统一 skill 强行重做账户中心。

覆盖限制：已有 user-validation.test.ts 主要覆盖注册/姓名/密码的一组断言，不等于手机号、邮箱、异步表单、清空、分页竞态或键盘交互已覆盖。DatePicker 等扩展控件在本次只核对 Form 的适配契约与组件文档，未逐一完成独立控件审计。新增页面真正使用时仍应读取对应实现；未执行测试不得标为通过。
