import { useEffect, useId, useRef, type ReactNode, type ComponentProps } from 'react'
import { FormProvider, useController, useWatch, type FieldPath, type FieldValues, type UseControllerProps, type UseFormReturn, type ControllerRenderProps } from 'react-hook-form'
import { Input } from './input'
import { Select, type ISelectOption } from './select'
import { Textarea } from './textarea'
import { Checkbox } from './checkbox'
import { Switch } from './switch'
import { RadioGroup } from './radio-group'
import { MultiSelect } from './multi-select'
import { DatePicker, parseDate } from './date-picker'
import { cn } from '@/lib/utils'

export type FormIssue<T extends FieldValues> = { name?: FieldPath<T>; message: string }
export type FormControlProps = {
  id: string
  disabled: boolean
  'aria-invalid': boolean
  'aria-required': boolean
  'aria-describedby'?: string
}

/** 默认文本输入；复杂控件通过 render 接入，字段名支持嵌套路径。 */
export type FormFieldConfig<T extends FieldValues> = {
  name: FieldPath<T>
  label: ReactNode
  description?: ReactNode
  descriptionTone?: 'muted' | 'error'
  required?: boolean
  rules?: UseControllerProps<T>['rules']
  dependencies?: FieldPath<T>[]
  visible?: (values: T) => boolean
  disabled?: boolean
  readOnly?: boolean
  kind?: 'input' | 'select' | 'checkbox' | 'textarea' | 'date' | 'multi-select' | 'radio' | 'switch'
  inputProps?: Pick<ComponentProps<typeof Input>, 'type' | 'placeholder' | 'autoComplete' | 'maxLength' | 'min' | 'max' | 'step' | 'clearable' | 'clearLabel'>
  textareaProps?: Pick<ComponentProps<typeof Textarea>, 'rows' | 'placeholder' | 'maxLength' | 'clearable' | 'clearLabel'>
  dateProps?: Pick<ComponentProps<typeof DatePicker>, 'min' | 'max' | 'placeholder' | 'clearable'>
  selectProps?: Pick<ComponentProps<typeof Select>, 'placeholder' | 'clearable' | 'clearValue'>
  multiSelectProps?: Pick<ComponentProps<typeof MultiSelect>, 'placeholder' | 'clearable'>
  radioProps?: Pick<ComponentProps<typeof RadioGroup>, 'orientation' | 'clearable'>
  options?: readonly ISelectOption[]
  className?: string
  render?: (context: { field: ControllerRenderProps<T>; controlProps: FormControlProps; form: UseFormReturn<T> }) => ReactNode
}

export interface IFormProps<T extends FieldValues> {
  form: UseFormReturn<T>
  fields: readonly FormFieldConfig<T>[]
  onSubmit: (values: T, form: UseFormReturn<T>) => void | Promise<void>
  /** 字段校验通过后执行，可访问整份数据及服务端业务检查；有错误则不提交。 */
  validate?: (values: T, signal: AbortSignal) => readonly FormIssue<T>[] | Promise<readonly FormIssue<T>[]>
  /** 将提交/业务检查异常映射为字段或表单级错误。不要返回敏感异常原文。 */
  mapError?: (cause: unknown) => readonly FormIssue<T>[]
  children?: ReactNode | ((form: UseFormReturn<T>) => ReactNode)
  className?: string
  fieldsClassName?: string
  'aria-label'?: string
}

function ConfiguredField<T extends FieldValues>({ config, form }: { config: FormFieldConfig<T>; form: UseFormReturn<T> }) {
  const id = useId()
  const originalValidate = config.rules?.validate
  const required = config.required ?? Boolean(config.rules?.required)
  const rules: UseControllerProps<T>['rules'] = {
    required: config.required ? '此项为必填。' : undefined,
    ...config.rules,
    validate: async (value, values) => {
      if (config.kind === 'date' && value) {
        if (typeof value !== 'string' || !parseDate(value)) return '请选择有效日期。'
        if (config.dateProps?.min && parseDate(config.dateProps.min) && String(value) < config.dateProps.min) return `日期不能早于 ${config.dateProps.min}。`
        if (config.dateProps?.max && parseDate(config.dateProps.max) && String(value) > config.dateProps.max) return `日期不能晚于 ${config.dateProps.max}。`
      }
      if (config.kind === 'multi-select' && !Array.isArray(value)) return '请选择有效选项。'
      if (!originalValidate) return true
      try {
        const validators = typeof originalValidate === 'function' ? [originalValidate] : Object.values(originalValidate)
        for (const validator of validators) {
          const result = await validator(value, values)
          if (result !== true && result !== undefined) return result
        }
        return true
      } catch { return '校验暂时失败，请重试。' }
    },
  }
  const { field, fieldState, formState } = useController({ name: config.name, control: form.control, rules, shouldUnregister: true })
  const dependencyValues = useWatch({ control: form.control, name: config.dependencies ?? [] })
  const previous = useRef(dependencyValues)
  useEffect(() => {
    const changed = dependencyValues.some((value, index) => !Object.is(value, previous.current[index]))
    previous.current = dependencyValues
    if (changed && (fieldState.isTouched || fieldState.error || formState.isSubmitted)) void form.trigger(config.name)
  }, [dependencyValues, fieldState.isTouched, fieldState.error, formState.isSubmitted, form, config.name])

  const controlProps: FormControlProps = {
    id, disabled: Boolean(config.disabled || formState.isSubmitting),
    'aria-invalid': Boolean(fieldState.error), 'aria-required': required,
    'aria-describedby': [config.description ? `${id}-hint` : '', fieldState.error ? `${id}-error` : ''].filter(Boolean).join(' ') || undefined,
  }
  let control: ReactNode
  if (config.render) control = config.render({ field, controlProps, form })
  else if (config.kind === 'select') control = <Select {...config.selectProps} {...controlProps} readOnly={config.readOnly} ref={field.ref} name={field.name} value={field.value ?? ''} onValueChange={field.onChange} onBlur={field.onBlur} options={config.options ?? []} className="w-full" />
  else if (config.kind === 'textarea') control = <Textarea {...config.textareaProps} {...field} {...controlProps} value={field.value ?? ''} readOnly={config.readOnly} />
  else if (config.kind === 'date') control = <DatePicker {...config.dateProps} {...controlProps} ref={field.ref} name={field.name} value={field.value ?? ''} onValueChange={field.onChange} onBlur={field.onBlur} readOnly={config.readOnly} />
  else if (config.kind === 'multi-select') control = <MultiSelect {...config.multiSelectProps} {...controlProps} ref={field.ref} name={field.name} value={Array.isArray(field.value) ? field.value : []} onValueChange={field.onChange} onBlur={field.onBlur} readOnly={config.readOnly} options={config.options ?? []} />
  else if (config.kind === 'radio') control = <RadioGroup {...config.radioProps} {...controlProps} aria-labelledby={`${id}-label`} ref={field.ref} name={field.name} value={field.value ?? ''} onValueChange={field.onChange} onBlur={(event) => { if (!event.currentTarget.contains(event.relatedTarget)) field.onBlur() }} readOnly={config.readOnly} options={config.options ?? []} />
  else if (config.kind === 'checkbox') control = <Checkbox {...controlProps} ref={field.ref} name={field.name} checked={Boolean(field.value)} onCheckedChange={(value) => field.onChange(value === true)} onBlur={field.onBlur} readOnly={config.readOnly} />
  else if (config.kind === 'switch') control = <Switch {...controlProps} ref={field.ref} name={field.name} checked={Boolean(field.value)} onCheckedChange={field.onChange} onBlur={field.onBlur} readOnly={config.readOnly} />
  else control = <Input {...config.inputProps} {...field} {...controlProps} value={field.value ?? ''} readOnly={config.readOnly} />

  return <div className={cn('space-y-2', config.className)}>
    {(config.kind === 'checkbox' || config.kind === 'switch') ? <div className="flex items-center gap-2">{control}<label id={`${id}-label`} htmlFor={id} className="text-sm">{config.label}{required && <span className="ml-1 text-error" aria-hidden="true">*</span>}</label></div>
      : <><label id={`${id}-label`} htmlFor={id} className="text-sm font-medium">{config.label}{required && <span className="ml-1 text-error" aria-hidden="true">*</span>}</label>{control}</>}
    {config.description && <p id={`${id}-hint`} className={cn('text-xs', config.descriptionTone === 'error' ? 'text-error' : 'text-muted-foreground')}>{config.description}</p>}
    {fieldState.isValidating && <p role="status" className="text-xs text-muted-foreground">正在校验…</p>}
    {fieldState.error && <p id={`${id}-error`} role="alert" className="text-sm text-error">{fieldState.error.message || '请检查此字段。'}</p>}
  </div>
}

/** 与现有 UI 共用主题；业务层持有 useForm 实例，可 reset/setValue/trigger/setError。 */
export function Form<T extends FieldValues>({ form, fields, onSubmit, validate, mapError, children, className, fieldsClassName, 'aria-label': label }: IFormProps<T>) {
  const values = useWatch({ control: form.control }) as T
  const lock = useRef(false)
  const request = useRef<AbortController | null>(null)
  const rootError = useRef<HTMLParagraphElement>(null)
  useEffect(() => () => { request.current?.abort() }, [])
  const visibleFields = fields.filter((item) => !item.visible || item.visible(values))
  function applyIssues(issues: readonly FormIssue<T>[]) {
    const generalMessages: string[] = []
    for (const issue of issues) {
      if (issue.name && visibleFields.some((item) => item.name === issue.name && !item.disabled)) {
        form.setError(issue.name, { type: 'business', message: issue.message })
      } else generalMessages.push(issue.message)
    }
    if (generalMessages.length) form.setError('root.business', { type: 'business', message: generalMessages.join('；') })
    // 等待提交禁用状态解除后再聚焦。
    setTimeout(() => {
      if (request.current?.signal.aborted) return
      const first = issues.find((issue) => issue.name && visibleFields.some((item) => item.name === issue.name && !item.disabled))
      if (first?.name) form.setFocus(first.name)
      else rootError.current?.focus()
    }, 0)
  }
  return <FormProvider {...form}>
    <form noValidate aria-label={label} aria-busy={form.formState.isSubmitting} className={cn('space-y-5', className)} onSubmit={async (event) => {
      event.preventDefault()
      if (lock.current) return
      lock.current = true
      request.current = new AbortController()
      const signal = request.current.signal
      form.clearErrors('root')
      try {
        await form.handleSubmit(async (data) => {
          const issues = await validate?.(data, signal) ?? []
          if (signal.aborted) return
          if (issues.length) { applyIssues(issues); return }
          await onSubmit(data, form)
        })(event)
      } catch (cause) {
        if (!signal.aborted) {
          let issues: readonly FormIssue<T>[] = []
          try { issues = mapError?.(cause) ?? [] } catch { /* 使用统一兜底提示 */ }
          applyIssues(issues.length ? issues : [{ message: '操作失败，请稍后重试。' }])
        }
      } finally { lock.current = false }
    }}>
      <div className={cn('space-y-5', fieldsClassName)}>
        {visibleFields.map((config) => <ConfiguredField key={config.name} config={config} form={form} />)}
      </div>
      {form.formState.errors.root?.business?.message && <p ref={rootError} tabIndex={-1} role="alert" className="text-sm text-error">{form.formState.errors.root.business.message}</p>}
      {typeof children === 'function' ? children(form) : children}
    </form>
  </FormProvider>
}
