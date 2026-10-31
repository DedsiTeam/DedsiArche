import { useForm } from 'react-hook-form'
import { RotateCcw, Search } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Select } from '@/components/ui/select'
import { Form, type FormFieldConfig } from '@/components/ui/form'
import {
  loginAuditReasonOptions, loginAuditResultOptions,
  type ILoginAuditFilters,
} from '@/services/login-audits'
import {
  isLoginAuditReason, isLoginAuditResult, normalizeLoginAuditTime, validateLoginAuditTime,
} from '@/lib/login-audit-validation'

type LoginAuditSearchValues = Omit<ILoginAuditFilters, 'page'>
const initialValues: LoginAuditSearchValues = {
  loginAccount: '', displayName: '', clientIp: '', result: '', reasonCode: '', from: '', to: '',
}

interface ILoginAuditFiltersProps {
  loading: boolean
  onSearch: (values: LoginAuditSearchValues) => void
}

/** 筛选草稿由表单持有；校验通过后才应用查询条件。 */
export function LoginAuditFilters({ loading, onSearch }: ILoginAuditFiltersProps) {
  const form = useForm<LoginAuditSearchValues>({
    defaultValues: initialValues,
    mode: 'onBlur',
    reValidateMode: 'onChange',
  })
  const busy = form.formState.isSubmitting || form.formState.isValidating
  const fields: FormFieldConfig<LoginAuditSearchValues>[] = [
    {
      name: 'loginAccount', label: '登录账号',
      inputProps: { maxLength: 200, placeholder: '登录账号', autoComplete: 'off' },
      rules: { maxLength: { value: 200, message: '账号筛选最多 200 个字符。' } },
    },
    {
      name: 'displayName', label: '姓名',
      inputProps: { maxLength: 100, placeholder: '姓名', autoComplete: 'off' },
      rules: { maxLength: { value: 100, message: '姓名筛选最多 100 个字符。' } },
    },
    {
      name: 'clientIp', label: '客户端 IP',
      inputProps: { maxLength: 64, placeholder: '客户端 IP', autoComplete: 'off' },
      rules: { maxLength: { value: 64, message: 'IP 筛选最多 64 个字符。' } },
    },
    {
      name: 'result', label: '登录结果', kind: 'select',
      options: loginAuditResultOptions,
      selectProps: { placeholder: '全部结果' },
      rules: { validate: (value) => isLoginAuditResult(String(value)) || '请选择有效登录结果。' },
    },
    {
      name: 'reasonCode', label: '结果原因', kind: 'select',
      options: loginAuditReasonOptions,
      selectProps: { placeholder: '全部原因' },
      rules: { validate: (value) => isLoginAuditReason(String(value)) || '请选择有效结果原因。' },
    },
    {
      name: 'from', label: '开始时间（北京时间，含）',
      inputProps: { type: 'datetime-local', step: 1, min: '0001-01-01T00:00', max: '9999-12-31T23:59:59' },
      rules: { validate: (value) => validateLoginAuditTime(String(value)) || true },
    },
    {
      name: 'to', label: '结束时间（北京时间，不含）', dependencies: ['from'],
      inputProps: { type: 'datetime-local', step: 1, min: '0001-01-01T00:00', max: '9999-12-31T23:59:59' },
      rules: { validate: (value, values) => {
        const message = validateLoginAuditTime(String(value))
        if (message) return message
        return !value || !values.from || normalizeLoginAuditTime(String(value)) > normalizeLoginAuditTime(values.from)
          || '结束时间须晚于开始时间。'
      } },
    },
  ]
  const order: (keyof LoginAuditSearchValues)[] = ['from', 'to', 'result', 'reasonCode', 'loginAccount', 'displayName', 'clientIp']
  const widths: Record<keyof LoginAuditSearchValues, string> = {
    from: 'w-60',
    to: 'w-60',
    result: 'w-36',
    reasonCode: 'w-44',
    loginAccount: 'w-44',
    displayName: 'w-32',
    clientIp: 'w-44',
  }
  // 标签保留给辅助技术，控件使用明确名称；错误仍在对应字段下方。
  const compactFields = order.map((name) => {
    const config = fields.find((field) => field.name === name)!
    return {
      ...config,
      className: `${widths[name]} shrink-0 space-y-0 [&>label]:sr-only`,
      render: ({ field, controlProps }) => config.kind === 'select' ? (
        <Select
          {...controlProps}
          {...config.selectProps}
          ref={field.ref}
          name={field.name}
          aria-label={String(config.label)}
          className="w-full"
          value={String(field.value ?? '')}
          onValueChange={field.onChange}
          onBlur={field.onBlur}
          options={config.options ?? []}
        />
      ) : (
        <Input
          {...config.inputProps}
          {...field}
          {...controlProps}
          aria-label={String(config.label)}
          title={name === 'from' || name === 'to' ? String(config.label) : name === 'clientIp' ? '客户端 IP，精确匹配' : `${config.label}，包含匹配`}
          value={String(field.value ?? '')}
          className="h-10 rounded-lg shadow-none"
        />
      ),
    } satisfies FormFieldConfig<LoginAuditSearchValues>
  })
  return (
    <Form
      form={form}
      fields={compactFields}
      aria-label="登录审计筛选"
      className="flex flex-wrap items-start gap-3 space-y-0 rounded-lg border bg-card p-4"
      fieldsClassName="contents space-y-0"
      renderFields={(items) => (
        <>
          <div className="flex flex-wrap items-start gap-2" role="group" aria-label="登录时间范围（北京时间，含开始、不含结束）" title="北京时间：包含开始时间，不包含结束时间，可单边筛选">
            {items.find((item) => item.name === 'from')?.node}
            <span aria-hidden="true" className="flex h-10 items-center text-sm text-muted-foreground">至</span>
            {items.find((item) => item.name === 'to')?.node}
          </div>
          {items.filter((item) => item.name !== 'from' && item.name !== 'to').map((item) => item.node)}
        </>
      )}
      onSubmit={(values) => onSearch({
        ...values,
        loginAccount: values.loginAccount.trim(),
        displayName: values.displayName.trim(),
        clientIp: values.clientIp.trim(),
        from: normalizeLoginAuditTime(values.from),
        to: normalizeLoginAuditTime(values.to),
      })}
    >
      <div className="flex shrink-0 items-center gap-3">
        <Button type="submit" className="h-10 rounded-lg px-5" disabled={loading || busy}>
          <Search aria-hidden="true" />搜索
        </Button>
        <Button type="button" variant="outline" className="h-10 rounded-lg px-4" disabled={loading || busy} onClick={() => {
          form.reset(initialValues)
          onSearch({ ...initialValues })
        }}>
          <RotateCcw aria-hidden="true" />重置
        </Button>
      </div>
    </Form>
  )
}
