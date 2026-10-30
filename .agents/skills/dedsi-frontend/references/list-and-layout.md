# 列表与布局

## 源码入口

| 文件 | 关注范围 |
| --- | --- |
| `src/react/src/pages/UsersPage.tsx` | 16–72 状态/请求；73–139 布局；141–147 弹窗成功回调 |
| `src/react/src/layouts/AppLayout.tsx` | 14–26 导航配置；44–79 工作区和左下账户区 |
| `src/react/src/components/SidebarMenu.tsx` | 多级菜单、选中规则、折叠浮层、可访问名称 |
| `src/react/src/styles/global.css` | 语义变量、PC 最小宽度、侧栏和页面内边距 |
| `src/react/src/components/ui/table.tsx` | Table 自带横向滚动容器 |
| `src/react/src/components/GlobalMessages.tsx`、`src/react/src/lib/notify.ts` | 唯一全局提示体系 |

## 布局与视觉基准

项目已确认的尺寸、主题规则以 `.agents/frontend.md` 为准。以下描述用于复刻 User 管理页，不把账户中心或登录样式混入列表。

| 区域 | 当前基准 |
| --- | --- |
| 工作区 | PC 最小宽度 1024px；右侧独立纵向滚动，内容占满；上下 20px、左右 24px，无 max-width 居中 |
| 侧栏 | 展开 232px，1024–1279px 为 208px，折叠 72px；深蓝背景；选中实蓝白字；账户区仅在展开时显示 |
| 页面 | `section.w-full.space-y-4`，通过 aria-labelledby 关联标题；标题简洁，不加副标题/装饰图标 |
| 标题行 | 最小高度 40px；左侧 24px 半粗标题，右侧主题色新增按钮，高 40px、圆角 8px |
| 搜索面板 | 与表格分开，白底、细边框、圆角 8px、内边距 16px；条件和搜索/重置靠左、gap 12px，可换行 |
| 表格面板 | 白底细边框、圆角 8px；表头浅灰、高 44px，数据行高 56px，单元格左右 12px |
| 表格尺寸 | 三个管理列表 w-full + table-auto，不设置大尺寸最小宽度；按内容自适应列宽，操作列按按钮实际宽度保留空间 |
| 行样式 | 隔行弱背景，hover 主题色低透明；ID 作为 React key，不用数组索引 |
| 分页 | 同一表格面板底部，顶部边框，16px 内边距；左总数和当前范围，右每页数、前后页、当前页和总页数 |

语义颜色：primary #2563EB、foreground #1E293B、muted-foreground #64748B、background #F3F4F6、card 白、border #E2E8F0、input #CBD5E1、error #B91C1C。通过 token/classes 使用，不在新页面重复散布色值。危险操作 token 与表单 error 不等同，不能因习惯把整个系统危险色改红。

## 筛选与请求状态

- 当前用户资料由认证模块及 RequireAuth 统一加载并缓存到 sessionStorage，列表只通过 useSession 读取能力；不要把 getProfile 放进依赖 filters/revision 的列表加载 effect。

- User 输入草稿 keyword/phone/email/status 与实际请求 filters 分开。打字或清空单字段只改草稿；点击搜索或 Enter 才提交，字符串 trim、page=1。不默默改成每次键入请求。
- 重置清空全部草稿及已提交条件，返回第一页并加载；没有独立刷新按钮。失败行的“重试”重新请求当前已提交条件。
- User 搜索框：账号/姓名 maxLength=100、宽 288px（xl 为 320px），手机号 maxLength=16、宽 176px，邮箱 maxLength=254、宽 224px，状态宽 160px。新领域按实际字段调整。
- 状态选择“全部”使用选项值 `all`，映射到页面空字符串，再映射请求 enabled 的省略值；true/false 显式转 boolean。`clearValue="all"` 保持清空等于全部，不把全部变成停用。
- 每次搜索、重置、重试和翻页清理旧错误并进入 loading；首屏 loading=true。旧响应通过 effect active 标记或等价请求序号丢弃，不能覆盖新筛选结果。
- `active=false` 只是忽略响应，不意味着已取消 HTTP。只读请求需要真正取消时在 service 透传 signal；不声称已取消已经提交的写操作。
- 页码从 1 开始，User 每页 10；total/size 从服务端结果取得。pageCount 至少为 1；总数为零显示 0，不显示负范围。翻页使用已提交筛选，非当前输入草稿。
- 删除后当前页超出最后一页时，回退后重新请求；整个回退请求周期保持 loading，旧数据与新页码不混用。当前 User 在此处有已记录差异。
- 查询失败保留面板内重试状态并提示；分页和新增在 loading 时禁用。无权访问显示明确说明与返回账户中心入口，不能伪装为空列表。

## 列展示与动作

- User 列顺序：ID、账号、姓名、手机号、邮箱、状态、创建时间、操作。账号带“本人”标签；手机号/邮箱为空显示 `—`，长文本 truncate 并提供 title；详情使用换行展示完整值。
- 三个管理列表第一列完整展示 ID；所有字段、表头和操作按钮保持单行；ID 和系统编码完整显示，不省略截断，空间不足时横向滚动。权限列表将所属系统拆为编码、名称两列，分别展示。
- 状态必须有中文文字，不能仅靠颜色或圆点。时间用本地字符串 `replace('T', ' ').slice(0, 19)`，数字/时间使用 tabular-nums。
- 操作列及表头通过 `table-action-column` 固定在横向滚动容器右侧，使用不透明背景并同步隔行与悬停状态，左侧显示分隔线；详情、编辑为主题色文字按钮，重置密码为次要动作，删除禁用本人。aria-label 包含操作和记录标识，避免一排同名按钮难以区分。
- 空、加载、失败三种状态使用横跨全部列的 TableCell，当前 User 高度 256px；增加列必须同步 colSpan 和 colgroup。
- Table 公共容器负责横向滚动，不能用隐藏溢出把宽列或操作裁掉。分页仍留在表格面板中。

## 菜单、反馈、可访问性

- 新页面挂到 `router/index.tsx` 的 RequireAuth → AppLayout 子路由；菜单在 AppLayout 的 Navigation 配置，复用 SidebarMenu 的嵌套数据结构。父菜单 children 和叶节点 to 互斥，id 稳定。
- 菜单显示取服务端能力值；当前只有 canManageUsers，不虚构 canManageSystems 等字段。新领域的管理权限先核对后端策略，再决定复用哪个能力入口。
- 保留折叠菜单的浮层导航、选中态、aria-current、aria-expanded、aria-controls 和图标按钮名称。内容区不重复创建第二个侧栏或顶部通栏。
- 成功/失败反馈用 notify，字段错误留在字段旁。现有提示居中顶部、最多三条，相同文本去重；成功/信息约 3.5 秒，错误约 5 秒。不新增第二套 Toast 或大块成功横幅。
- 保留键盘焦点、表单可访问名称、标签关联、加载 aria-busy、只读状态及 reduced-motion。没有浏览器验证时只报告源码审阅，不宣称键盘/视觉表现已经通过。
