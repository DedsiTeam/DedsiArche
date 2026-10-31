import { useId } from 'react'
import { Controller, useFieldArray, type UseFormReturn } from 'react-hook-form'
import { Plus, Trash2 } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Checkbox } from '@/components/ui/checkbox'
import {
  validateDictionaryItemCode, validateDictionarySort, validateDictionaryText,
  type IDictionaryFormValues,
} from '@/lib/dictionary-validation'

interface IDictionaryItemsEditorProps {
  form: UseFormReturn<IDictionaryFormValues>
  disabled: boolean
  /* 初始已有编码不能通过移除旧项、再新增同编码项来替换身份。 */
  originalCodes: readonly string[]
}

/** 字典项属于当前聚合；所有字段注册到外层公共 Form，不创建嵌套表单。 */
export function DictionaryItemsEditor({ form, disabled, originalCodes }: IDictionaryItemsEditorProps) {
  const { fields, append, remove } = useFieldArray({ control: form.control, name: 'items' })
  const idPrefix = useId()

  function setDefault(index: number, selected: boolean) {
    // 默认项切换是显式操作；其他项取消默认，不改变其启用状态。
    form.getValues('items').forEach((_, other) => {
      form.setValue(`items.${other}.isDefault`, selected && other === index, { shouldDirty: true, shouldValidate: true })
    })
  }

  return (
    <section aria-label="字典项" className="flex min-h-0 min-w-0 flex-col gap-3 border-l py-1 pl-5">
      <div className="flex shrink-0 items-center justify-between gap-3">
        <h2 className="text-sm font-semibold" aria-live="polite">字典项 <span className="ml-1 rounded-md bg-primary/10 px-2 py-0.5 text-xs tabular-nums text-primary">{fields.length}</span></h2>
        <Button type="button" size="sm" variant="outline" disabled={disabled} onClick={() => append({
          code: '', name: '', sortOrder: '0', isDefault: false, enabled: true,
        }, { focusName: `items.${fields.length}.code` })}>
          <Plus aria-hidden="true" />新增字典项
        </Button>
      </div>
      <p className="shrink-0 text-xs text-muted-foreground">编码在当前字典内唯一；已有项编码不可修改。最多设置一个默认项。</p>
      <div className="min-h-0 flex-1 space-y-3 overflow-y-auto overscroll-contain pr-1">
        {fields.length === 0 ? (
          <div className="flex min-h-40 items-center justify-center rounded-lg border border-dashed text-sm text-muted-foreground">暂无字典项，可新增或保存为空字典。</div>
        ) : fields.map((item, index) => {
          const rowId = `${idPrefix}-${item.id}`
          return (
            <fieldset key={item.id} disabled={disabled} className="space-y-3 rounded-lg border bg-card p-3">
              <legend className="sr-only">第 {index + 1} 个字典项</legend>
              <div className="flex items-center justify-between gap-2">
                <span className="text-xs font-medium text-muted-foreground">字典项 {index + 1}{item.itemId ? '' : ' · 新增'}</span>
                <Button type="button" size="sm" variant="ghost" disabled={disabled} aria-label={`移除第 ${index + 1} 个字典项`} onClick={() => {
                  remove(index)
                  form.clearErrors('items')
                }}><Trash2 aria-hidden="true" />移除</Button>
              </div>
              <div className="grid grid-cols-2 gap-3">
                <Controller
                  control={form.control}
                  name={`items.${index}.code`}
                  rules={{ validate: (value) => {
                    if (!item.itemId && originalCodes.includes(value.trim())) {
                      return '已有编码请保留原字典项；如已移除，请取消并重新打开编辑。'
                    }
                    return validateDictionaryItemCode(value, index, form.getValues('items')) || true
                  } }}
                  render={({ field, fieldState }) => (
                    <div className="space-y-1.5">
                      <label htmlFor={`${rowId}-code`} className="text-xs font-medium">编码<span className="ml-1 text-error" aria-hidden="true">*</span></label>
                      <Input {...field} id={`${rowId}-code`} disabled={disabled} readOnly={Boolean(item.itemId)} maxLength={100} autoComplete="off" aria-required aria-invalid={Boolean(fieldState.error)} aria-describedby={fieldState.error ? `${rowId}-code-error` : undefined} />
                      {fieldState.error && <p id={`${rowId}-code-error`} role="alert" className="text-xs text-error">{fieldState.error.message}</p>}
                    </div>
                  )}
                />
                <Controller
                  control={form.control}
                  name={`items.${index}.name`}
                  rules={{ validate: (value) => validateDictionaryText(value, '字典项名称') || true }}
                  render={({ field, fieldState }) => (
                    <div className="space-y-1.5">
                      <label htmlFor={`${rowId}-name`} className="text-xs font-medium">名称<span className="ml-1 text-error" aria-hidden="true">*</span></label>
                      <Input {...field} id={`${rowId}-name`} disabled={disabled} maxLength={100} autoComplete="off" aria-required aria-invalid={Boolean(fieldState.error)} aria-describedby={fieldState.error ? `${rowId}-name-error` : undefined} />
                      {fieldState.error && <p id={`${rowId}-name-error`} role="alert" className="text-xs text-error">{fieldState.error.message}</p>}
                    </div>
                  )}
                />
              </div>
              <div className="grid grid-cols-[minmax(100px,1fr)_minmax(110px,1fr)_minmax(110px,1fr)] items-start gap-3">
                <Controller
                  control={form.control}
                  name={`items.${index}.sortOrder`}
                  rules={{ validate: (value) => validateDictionarySort(value) || true }}
                  render={({ field, fieldState }) => (
                    <div className="space-y-1.5">
                      <label htmlFor={`${rowId}-sort`} className="text-xs font-medium">排序<span className="ml-1 text-error" aria-hidden="true">*</span></label>
                      <Input {...field} id={`${rowId}-sort`} type="number" min={0} max={2147483647} step={1} disabled={disabled} aria-required aria-invalid={Boolean(fieldState.error)} aria-describedby={fieldState.error ? `${rowId}-sort-error` : undefined} />
                      {fieldState.error && <p id={`${rowId}-sort-error`} role="alert" className="text-xs text-error">{fieldState.error.message}</p>}
                    </div>
                  )}
                />
                <Controller
                  control={form.control}
                  name={`items.${index}.enabled`}
                  render={({ field }) => (
                    <div className="space-y-2 pt-6">
                      <div className="flex items-center gap-2">
                        <Checkbox ref={field.ref} id={`${rowId}-enabled`} name={field.name} checked={field.value} disabled={disabled} onBlur={field.onBlur} onCheckedChange={(checked) => {
                          field.onChange(checked === true)
                          if (checked !== true) form.setValue(`items.${index}.isDefault`, false, { shouldDirty: true, shouldValidate: true })
                        }} aria-describedby={`${rowId}-enabled-hint`} />
                        <label htmlFor={`${rowId}-enabled`} className="text-xs">启用</label>
                      </div>
                      <p id={`${rowId}-enabled-hint`} className="text-xs text-muted-foreground">停用会取消默认。</p>
                    </div>
                  )}
                />
                <Controller
                  control={form.control}
                  name={`items.${index}.isDefault`}
                  rules={{ validate: (value) => {
                    if (!value) return true
                    if (!form.getValues(`items.${index}.enabled`)) return '默认项必须启用。'
                    return form.getValues('items').filter((entry) => entry.isDefault).length <= 1 || '最多设置一个默认项。'
                  } }}
                  render={({ field, fieldState }) => (
                    <div className="space-y-2 pt-6">
                      <div className="flex items-center gap-2">
                        <Checkbox ref={field.ref} id={`${rowId}-default`} name={field.name} checked={field.value} disabled={disabled} onBlur={field.onBlur} onCheckedChange={(checked) => setDefault(index, checked === true)} aria-invalid={Boolean(fieldState.error)} aria-describedby={fieldState.error ? `${rowId}-default-error` : undefined} />
                        <label htmlFor={`${rowId}-default`} className="text-xs">默认项</label>
                      </div>
                      {fieldState.error && <p id={`${rowId}-default-error`} role="alert" className="text-xs text-error">{fieldState.error.message}</p>}
                    </div>
                  )}
                />
              </div>
            </fieldset>
          )
        })}
      </div>
    </section>
  )
}
