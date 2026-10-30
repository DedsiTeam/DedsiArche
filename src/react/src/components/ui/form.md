# 统一 Form

文本输入、文本域、下拉、日期、多选默认使用主题 `bg-card` 背景，只读或禁用时使用 `bg-muted`。背景由公共组件提供，普通业务页面无需单独设置白底；复选框、单选和开关保留选中状态颜色。

`form.tsx` 提供项目 UI 与 React Hook Form 的适配，不绑定业务 API，不依赖 Zod。
真实使用示例：[UserFormDialog.tsx](../UserFormDialog.tsx)。

## 基本使用

```tsx
import { useForm } from 'react-hook-form'
import { Form, type FormFieldConfig } from '@/components/ui/form'
import { Button } from '@/components/ui/button'

type Values = { name: string; status: string }

function Editor({ initialValues, save }: {
  initialValues: Values
  save: (values: Values) => Promise<void>
}) {
  const form = useForm<Values>({
    defaultValues: initialValues,
    mode: 'onBlur',
    reValidateMode: 'onChange',
  })
  const fields: FormFieldConfig<Values>[] = [
    {
      name: 'name', label: '名称', required: true,
      rules: {
        maxLength: { value: 100, message: '最多 100 个字符。' },
        validate: (value) => Boolean(String(value).trim()) || '名称不能只有空格。',
      },
    },
    {
      name: 'status', label: '状态', kind: 'select', required: true,
      options: [{ value: 'active', label: '启用' }, { value: 'inactive', label: '停用' }],
    },
  ]
  return <Form form={form} fields={fields} onSubmit={save}>
    {({ formState }) => <Button type="submit" disabled={formState.isSubmitting}>
      {formState.isSubmitting ? '正在保存…' : '保存'}
    </Button>}
  </Form>
}
```

`required` 默认同时启用 RHF 必填校验和必填标记，`rules.required` 可覆盖提示或规则。必填字符串需要拒绝纯空格时，应另配 `validate`。不自动裁剪或转换值，尤其不能裁剪密码。

## 配置与绑定

| 配置 | 作用 |
| --- | --- |
| `name` | 类型检查的 RHF 字段路径，支持 `profile.name` 等嵌套对象 |
| `label / description` | 标签、辅助说明，自动关联控件与错误提示 |
| `descriptionTone` | 默认 muted；error 将指定规则说明标红，适用于密码规则提示 |
| `kind` | `input`、`select`、`textarea`、`date`、`multi-select`、`radio`、`switch`、`checkbox` |
| `inputProps` | 文本框类型、占位文字、自动填充等 |
| `options` | Select、MultiSelect、RadioGroup 的选项；value 为非空唯一字符串，可设置 disabled |
| `rules` | RHF required/min/max/minLength/maxLength/pattern/validate 等规则 |
| `dependencies` | 当前字段依赖的其他字段；依赖改变时，已触碰、有错误或已提交的当前字段重新校验 |
| `visible(values)` | 条件显示；隐藏会卸载、注销字段，清除其错误和当前值，不参与提交 |
| `disabled` | 禁用交互，但保留已绑定数据；提交时仍校验 |
| `readOnly` | 所有内置控件支持只读，保留绑定值 |
| `render` | 接入自定义控件，获得 `field`、`controlProps`、`form` |
| `className` | 字段容器样式 |

Form 的 `fieldsClassName` 可设置字段容器布局，例如 `grid grid-cols-2 gap-5 space-y-0`；业务字段可用 `className: 'col-span-2'` 跨列。`children` 用于底部按钮，也可传入取得 form 实例的渲染函数。

业务层持有 `useForm` 实例：

- `form.reset(record)`：回填或重置。`defaultValues` 只在初次初始化生效，不会随着 props 自动覆盖用户输入。
- `form.setValue('name', value, { shouldDirty: true, shouldValidate: true })`：程序更新值。
- `form.watch` / `useWatch`：订阅数据，驱动关联 UI。
- `form.trigger()`：运行字段规则（不运行 Form 的提交前 `validate`）。
- `form.setError` / `clearErrors`：手动错误管理；清除错误不等于通过校验。
- 弹窗切换记录时可重新挂载或调用 `reset`。当前用户弹窗关闭时卸载，避免下次打开保留密码。

## 复杂字段校验

`rules.validate` 支持同步函数、异步函数和多个命名校验器。函数接收字段值和完整表单值，返回 `true`/`undefined` 表示成功，字符串表示错误。抛出异常会转成“校验暂时失败，请重试”，不会被当成通过。

```tsx
// Values 中包含 password、confirmPassword
{
  name: 'confirmPassword', label: '确认密码', required: true,
  inputProps: { type: 'password', autoComplete: 'new-password' },
  dependencies: ['password'],
  rules: { validate: (value, values) => value === values.password || '两次密码不一致。' },
}
```

复杂字段可配置命名规则：

```tsx
rules: {
  validate: {
    format: (value) => isValidFormat(value) || '格式不正确。',
    available: async (value, values) =>
      (await checkAvailability(value, values)) || '当前值不可用。',
  },
}
```

上面的 `isValidFormat`、`checkAvailability` 由业务代码实现，API 调用仍放 services 层。异步字段验证可能在失焦、依赖变化、提交时执行，接口应只读。昂贵、整单级的检查优先放在提交前业务校验；本组件没有隐含的网络请求或自动防抖。

## 提交前业务校验和服务端错误

调用顺序：字段规则 → `validate(values, signal)` → `onSubmit(values, form)`。

```tsx
<Form
  form={form}
  fields={fields}
  validate={async (values, signal) => {
    // checkBusiness 是业务服务函数，应传递 signal，不能在这里执行写操作。
    const result = await checkBusiness(values, { signal })
    if (!result.allowed) return [{ name: 'name', message: '当前业务状态不允许此名称。' }]
    return []
  }}
  onSubmit={save}
  mapError={(error) => {
    // 根据接口明确的业务错误码映射，不能把所有失败都归为字段错误。
    if (isNameConflict(error)) return [{ name: 'name', message: '名称已存在。' }]
    return [{ message: '保存失败，请稍后重试。' }]
  }}
/>
```

`validate` 返回 `{ name?, message }[]`，非空会阻止写入；省略 name 表示表单级错误。异步检查期间仍处于提交中，统一禁用内置字段，使用同步提交锁防止重复提交。卸载时中止业务检查信号，不再调用后续提交。已经开始的写请求不由组件擅自取消。

`validate` 或 `onSubmit` 抛出的异常通过 `mapError` 映射；未映射、返回空数组或映射自身失败时，显示通用失败提示，不暴露异常原文。隐藏或不可操作字段的错误展示在表单底部，避免错误不可见。组件负责定位可操作字段；表单级错误有 `role="alert"` 和焦点。

前端校验不能替代后端授权、唯一约束或并发版本校验。业务预检查通过后，保存仍可能冲突，必须继续处理提交接口错误。`UserFormDialog` 演示了停用确认、当前账户保护、用户名冲突和版本冲突处理。

## 自定义控件

`render` 中把 `field.value/onChange/onBlur/ref` 连接到真实可聚焦控件，并透传 `controlProps` 的 id、disabled 和 ARIA 属性。受控 Select 已支持 ref 与 onBlur。布尔下拉框、数字输入需要显式转换；默认 Input 保持浏览器字符串值。

```tsx
render: ({ field, controlProps }) => <Select
  {...controlProps}
  ref={field.ref}
  name={field.name}
  value={String(field.value)}
  onValueChange={(value) => field.onChange(value === 'true')}
  onBlur={field.onBlur}
  options={[{ value: 'true', label: '启用' }, { value: 'false', label: '停用' }]}
/>
```

自定义控件必须遵循 disabled，避免业务异步检查时修改本次待提交数据。本版提供对象路径和条件字段；可排序动态字段数组需要专门适配 `useFieldArray` 的稳定 key 与注销行为，不应直接依赖字段路径作为数组行 key。

## 密码生成输入框

`PasswordGeneratorInput` 保留 Input 的 id、ref、onBlur 和 ARIA 属性，使用 `value` / `onValueChange` 绑定值。可在 Form 字段的 render 中使用，也可绑定普通 React state。

输入框右侧提供显示／隐藏密码按钮，默认隐藏，关闭并重新打开弹窗后恢复隐藏。切换显隐不改变字段值、复制内容或校验状态。

点击生成只填入密码字段；`onGenerate` 用于清空旧确认密码，确认值仍由用户点击复制后手动粘贴。复制通过 Clipboard API 完成，失败会显示提示，不会假报成功。生成使用 Web Crypto，固定 20 位 ASCII，包含小写、大写、数字、特殊符号各至少一位；不使用 Math.random，不记录密码或写入本地存储。

公共 Input 与 Select 默认支持清空，清空沿用正常变更回调，绑定值及 RHF 脏状态同步更新；校验仍遵循表单现有 mode/reValidateMode。Input 字段可通过 inputProps.clearable=false 关闭。业务上不可为空的枚举控件应显式关闭清空，避免将空字符串误转换为 false。

## 补齐的表单控件

所有控件沿用当前蓝灰色系，通过 Form 自动连接 value/onChange/onBlur/ref 和错误描述、必填标记、提交禁用。清空不绕过规则：沿用 useForm 的 mode/reValidateMode，提交时仍会校验。独立使用 DatePicker、MultiSelect、RadioGroup 时传入受控 value/onValueChange。

| 组件 | kind | 值 / 空值 | 配置 |
| --- | --- | --- | --- |
| Textarea 多行文本 | textarea | string / '' | textareaProps: rows、placeholder、maxLength、clearable、clearLabel |
| DatePicker 日期 | date | YYYY-MM-DD / '' | dateProps: min、max、placeholder、clearable |
| MultiSelect 多选 | multi-select | string[] / [] | options；multiSelectProps: placeholder、clearable |
| RadioGroup 单选组 | radio | string / '' | options；radioProps: orientation、clearable |
| Switch 开关 | switch | boolean，默认 false | disabled、readOnly、rules |
| Checkbox 复选框 | checkbox | boolean，默认 false | disabled、readOnly、rules |

`selectProps` 可配置已有 Select 的 placeholder、clearable、clearValue。不要通过清空把账户状态等业务枚举转换为 false；这种场景请关闭清空。

- 日期不含时间或时区。Form 默认检查日期是否合法及 dateProps.min/max；跨字段日期关系使用 dependencies + rules.validate。min/max 是闭区间，清空为 ''。
- 日历支持年份/月选择、上下月、方向键、Home/End、PageUp/PageDown、Enter/空格选择与 Esc 关闭。已保存的越界值会保留显示，但不能通过表单验证，不会静默改值。
- 多选必填要求数组至少一项；数量限制、选项组合等使用 rules.validate。空数组必须在 defaultValues 中显式初始化。
- 开关和复选框的 false 是合法布尔值；设置 required 会要求为 true，适用于“同意/确认”。普通启停开关不应设置 required。
- 只读/禁用不显示清空按钮；布尔控件以禁用交互实现只读，但 Form 仍保留绑定数据。提交期间全部内置控件禁止修改。
- 多行文本的 maxLength 属性用于限制输入，若程序回填也需校验长度，应同时配置 rules.maxLength。
- 复选框独立组件支持半选态，Form 的 checkbox 绑定只采用 boolean。

完整且参与 TypeScript 构建的示例：[FormComponentsExample.tsx](../examples/FormComponentsExample.tsx)。示例包含六类控件、必填、最大长度、多选数量、跨字段日期校验、整单业务校验注入与重置；由调用方提供 save / validateBusiness，不内置接口或模拟保存，不自动添加业务菜单。

```tsx
const fields: FormFieldConfig<Values>[] = [
  { name: 'notes', label: '备注', kind: 'textarea', textareaProps: { rows: 4 } },
  { name: 'date', label: '日期', kind: 'date', required: true },
  { name: 'tags', label: '标签', kind: 'multi-select', options },
  { name: 'category', label: '分类', kind: 'radio', options, required: true },
  { name: 'enabled', label: '启用', kind: 'switch' },
  { name: 'confirmed', label: '确认信息', kind: 'checkbox', required: true },
]
// Values 字段类型和 defaultValues 必须对应上表，options 使用唯一字符串 value。
```

本次未提供文件上传组件。

### 自定义字段分区

`renderFields` 接收已通过 `visible` 筛选的 `{ name, node }[]`，可按字段名将 `node` 放入不同分区。每个可见字段必须且只能渲染一次，不重新创建独立表单。字段绑定、隐藏注销、校验及提交仍由同一个 Form 负责；未提供时保持原有纵向布局。用户表单使用此入口实现左侧基本信息、右侧岗位选择。
