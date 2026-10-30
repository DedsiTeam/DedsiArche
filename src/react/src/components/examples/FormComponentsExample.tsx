import { useForm } from 'react-hook-form'
import { Form, type FormFieldConfig, type FormIssue } from '@/components/ui/form'
import { Button } from '@/components/ui/button'

export interface IFormExampleValues {
  description: string
  startDate: string
  endDate: string
  channels: string[]
  priority: string
  enabled: boolean
  confirmed: boolean
}
const initialValues: IFormExampleValues = {
  description: '', startDate: '', endDate: '', channels: [], priority: '', enabled: false, confirmed: false,
}

/** 可直接嵌入页面或 Dialog；示例不调用真实接口、不自动保存数据。 */
export function FormComponentsExample({ save, validateBusiness }: {
  save: (values: IFormExampleValues) => Promise<void>
  validateBusiness?: (values: IFormExampleValues, signal: AbortSignal) => Promise<readonly FormIssue<IFormExampleValues>[]>
}) {
  const form = useForm<IFormExampleValues>({ defaultValues: initialValues, mode: 'onBlur', reValidateMode: 'onChange' })
  const fields: FormFieldConfig<IFormExampleValues>[] = [
    { name: 'description', label: '说明', kind: 'textarea', required: true, textareaProps: { rows: 4, maxLength: 500 },
      rules: { maxLength: { value: 500, message: '说明最多 500 个字符。' }, validate: (value) => !!String(value).trim() || '请输入说明。' } },
    { name: 'startDate', label: '开始日期', kind: 'date', required: true, dateProps: { min: '2020-01-01', max: '2099-12-31' } },
    { name: 'endDate', label: '结束日期', kind: 'date', required: true, dependencies: ['startDate'],
      dateProps: { min: '2020-01-01', max: '2099-12-31' },
      rules: { validate: (value, values) => !values.startDate || String(value) >= values.startDate || '结束日期不能早于开始日期。' } },
    { name: 'channels', label: '通知渠道', kind: 'multi-select', required: true,
      options: [{ value: 'email', label: '邮箱' }, { value: 'sms', label: '短信' }, { value: 'inbox', label: '站内消息' }],
      rules: { validate: (value) => (Array.isArray(value) && value.length <= 2) || '最多选择两个渠道。' } },
    { name: 'priority', label: '优先级', kind: 'radio', required: true,
      options: [{ value: 'normal', label: '普通' }, { value: 'urgent', label: '紧急' }] },
    { name: 'enabled', label: '启用通知', kind: 'switch' },
    { name: 'confirmed', label: '我已确认以上信息', kind: 'checkbox', required: true },
  ]
  return <Form form={form} fields={fields} validate={validateBusiness} onSubmit={save} aria-label="表单组件示例">
    {({ formState }) => <div className="flex gap-3">
      <Button type="submit" disabled={formState.isSubmitting}>{formState.isSubmitting ? '正在保存…' : '保存'}</Button>
      <Button type="button" variant="outline" disabled={formState.isSubmitting} onClick={() => form.reset(initialValues)}>重置</Button>
    </div>}
  </Form>
}
