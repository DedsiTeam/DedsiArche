# 表单与弹窗

## 入口与复用边界

- 新增/编辑基准：`src/react/src/components/UserFormDialog.tsx`。
- 详情/重置/删除当前实现：`src/react/src/components/UserActionDialog.tsx`；它还未统一到 Form，见问题清单，不能把旧实现当成新的表单模板。
- 公共契约：`src/react/src/components/ui/form.tsx` 与同目录 `form.md`。修改字段或控件前阅读其中对应段落；六类扩展示例为 `src/react/src/components/examples/FormComponentsExample.tsx`，不将示例自动挂为业务菜单。

## 表单状态与配置

- 业务组件持有 useForm；mode=onBlur、reValidateMode=onChange。defaultValues 完整声明字符串空值 `''`、多选 `[]`、布尔值 true/false，避免受控/非受控切换。
- defaultValues 只在初始化使用。切换记录通过卸载重建或 reset，不能期待 props 更新自动回填；异步回填不能无条件覆盖用户正在编辑的值。
- 字段配置使用 `FormFieldConfig<T>[]`，name 为真实字段路径；业务输入值与 API DTO 分开，保存时显式选择字段，不展开整单数据发送确认密码/确认勾选等 UI 字段。
- required 负责标记及默认必填规则；拒绝纯空白另加 validate。规则可返回 true/undefined 或中文错误；旧工具函数以空字符串表示通过时转为 `validator(value) || true`。
- rules 用于字段校验，dependencies 声明跨字段依赖；已触碰、有错误或已提交的字段会随依赖重校验。整单检查用 Form.validate，接口异常用 mapError。
- 条件字段用 visible；隐藏会卸载并注销值/错误，不参与提交。后续显现时按业务需要初始化；不依赖已隐藏字段的旧值。保留值的不可编辑字段用 readOnly，不用隐藏代替。
- 配置 disabled 只禁用交互，当前实现保留数据并继续校验。readOnly 在内置控件中保留值；自定义 render 必须自行传递只读语义，controlProps 不包含 readOnly。

## 控件适配

文本输入、文本域、Select、DatePicker、MultiSelect 的可编辑主体由公共组件默认提供 `bg-card`，只读/禁用为 `bg-muted`；不要为普通页面重复补白底或写死 `bg-white`。原生控件使用 read-only/disabled 状态，日期和多选使用 aria-readonly，Select 的只读映射到 disabled。勾选类控件保留选中颜色；按钮、菜单和 Tabs 的有意透明背景不适用此规则。

| kind | 表单值 | 注意 |
| --- | --- | --- |
| input / textarea | string，空为 '' | maxLength 限制键入不代替 rules 长度校验；数字输入默认仍是字符串 |
| select / radio | string，空为 '' | options.value 唯一非空；不可为空的状态明确 clearable=false |
| date | YYYY-MM-DD，空为 '' | 校验真实日期和 min/max；不作为时间戳，不按浏览器时区转换 |
| multi-select | string[]，空为 [] | 必填至少一项；数量和组合需业务 rules |
| switch / checkbox | boolean | false 合法；required 意味着必须 true，普通启停不可误设 required |

- 自定义 render 连接 value/onChange/onBlur/ref、真实可聚焦控件的 id，以及 controlProps 的 disabled/ARIA。boolean Select 显式在字符串与 boolean 间转换。
- 公共 Input、Select 默认支持清空；Input 定宽通常设 containerClassName，内部输入设 w-full，防止外包装仍占满行。清空走原变更回调，保持 RHF 值一致。
- 只读/禁用隐藏清空；筛选 Select 清空可返回 all，编辑状态清空不可隐式变 false。按钮显式 type=button，避免触发表单提交。
- 密码显隐使用 hasEndAction，为显隐、清空分别留位置。不要另造密码输入框；生成/复制流程用 PasswordGeneratorInput。
- 复用组件的键盘、focus、ref 与 ARIA，不用原生 select 替换 Radix Select。文件上传、可排序字段数组未由当前 Form 完整提供，实际需求出现时单独适配，不虚构现成能力。

## 提交与校验流程

1. Form 同步 ref 锁防重复提交；字段规则通过后，执行 `validate(values, signal)`。
2. 业务检查返回 `{ name?, message }[]`；非空则显示错误并停止写入。预检查只读，不能在校验阶段保存。
3. 再执行 onSubmit；异常交给 mapError。字段错误映射到可见可操作字段，其他显示表单底部；保留用户输入。
4. 成功后由父页面关闭弹窗、notify.success、重新加载列表；涉及当前账户资料时刷新 profile 以更新侧栏。失败不先关闭、不虚报成功。

- 提交过程（含提交前异步业务检查）字段与按钮禁用，关闭按钮隐藏，Esc/遮罩/取消不能关闭。校验失败解除禁用后定位首个错误字段或表单级错误。
- 失焦异步字段校验也需符合项目的禁用与防误关闭要求；UserFormDialog 已同时检查 isSubmitting 和 isValidating；其他调用方应独立核对。
- validate 的 AbortSignal 在卸载时中止；异步 service 必须透传才真正取消请求。已经开始的写入不会由 Form 自动撤销。
- 错误文字/必填星号用 text-error，控件 aria-invalid 触发红边框；说明文字默认 muted，仅密码规则等显式 descriptionTone=error。自定义控件应同时关联说明和错误。

## 弹窗外观和生命周期

- 复用 Dialog/Content/Header/Title。当前默认宽 560px、最大高 85vh、内部滚动、padding 24px、gap 24px，标题 20px。复用公共组件，特殊宽度必须由字段布局需要决定。
- 管理弹窗仅主标题，没有说明副标题时设 aria-describedby={undefined}；字段规则贴近字段，删除后果在确认正文。
- 通过条件渲染挂载；关闭后卸载以清除密码、确认值和错误。提交完成后刷新列表；关闭本身不能触发写操作。
- 详情从详情接口读取最新数据，显示加载/失败/重试；不直接把列表快照当详情。用户编辑通过 UserEditDialog 先加载完整详情（含岗位快照），成功后挂载表单；加载中或失败不提供保存入口，关闭中止请求。
- 删除先显示对象标识和不可恢复后果，确认后带 version 删除；用户详情/删除会先拉取最新详情。不要以 window.confirm 替代现有 Dialog。
- 按钮排列现状不统一：新增/编辑在左，删除确认在右。采用对应操作的既有排列；未获统一设计要求时，不擅自宣布所有弹窗必须同一方向。

## User 专属字段及行为

| 字段/场景 | 当前契约 |
| --- | --- |
| 账号 | 新增必填，trim 后 3–50 位 ASCII 字母数字下划线；编辑只读，不发送编辑 username |
| 姓名 | trim 后非空，最多 100 字符 |
| 手机号 | 可空，trim 后可选 + 和 7–15 位数字；输入最多 16 字符 |
| 邮箱 | 可空，trim 后最多 254 字符，复用 validateEmail |
| 新增密码 | 12–64 个字符、UTF-8 最多 72 字节，非纯空白；传输原值，不 trim |
| 确认密码 | 必填，与密码完全一致，不发给后端；依赖 password |
| 账户状态 | 只在编辑显示，当前账户不能停用；只读保护仍须后端实施 |
| 停用确认 | 原启用改为停用才显示；未确认不能保存；状态改动后重置确认 |

- 创建时不发送状态，沿用后端创建默认；编辑带 version，手机号/邮箱清空发送空串，不随意改成省略/null。
- 密码生成固定 20 位，四类字符至少各一位，使用现有 Web Crypto 实现；只填密码并清旧确认。复制成功后由用户手动粘贴确认，不能自动替用户确认。
- 复制失败如实通知；密码不进入日志、URL、持久化存储、示例数据。密码显隐初始隐藏，重新打开恢复隐藏。
- 重置本人密码后清除本地会话，提示重新登录；删除本人禁用，受保护管理员由后端判断。不要把这些 User 特例复制到权限或系统管理。
