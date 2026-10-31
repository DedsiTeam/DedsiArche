import { useForm } from 'react-hook-form'
import { isAxiosError } from 'axios'
import { Form, type FormFieldConfig, type FormIssue } from '@/components/ui/form'
import { Button } from '@/components/ui/button'
import { Dialog, DialogContent, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { PermissionSystemSelect } from '@/components/PermissionSystemSelect'
import { DictionaryItemsEditor } from '@/components/DictionaryItemsEditor'
import { createDictionary, editDictionary, dictionaryEnabledOptions, type IDictionary } from '@/services/dictionaries'
import { dictionaryError, validateDictionaryText, type IDictionaryFormValues } from '@/lib/dictionary-validation'

interface IDictionaryFormDialogProps {
  dictionary: IDictionary | null
  onClose: () => void
  onSuccess: (message: string) => void
}

/** 基本资料和内部子实体共用一个 Form，保存时作为完整聚合提交。 */
export function DictionaryFormDialog({ dictionary, onClose, onSuccess }: IDictionaryFormDialogProps) {
  const form = useForm<IDictionaryFormValues>({
    mode: 'onBlur',
    reValidateMode: 'onChange',
    defaultValues: {
      systemCode: dictionary?.systemCode ?? '',
      code: dictionary?.code ?? '',
      name: dictionary?.name ?? '',
      description: dictionary?.description ?? '',
      enabled: String(dictionary?.enabled ?? true),
      items: dictionary?.items.map((item) => ({
        // RHF 的渲染 id 与后端主键分开保存，避免增删行时覆盖真实主键。
        itemId: item.id,
        code: item.code,
        name: item.name,
        sortOrder: String(item.sortOrder),
        isDefault: item.isDefault,
        enabled: item.enabled,
      })) ?? [],
    },
  })
  const busy = form.formState.isSubmitting || form.formState.isValidating
  const fields: FormFieldConfig<IDictionaryFormValues>[] = [
    {
      name: 'systemCode', label: '所属系统', required: true,
      visible: () => !dictionary,
      description: '创建后不能修改所属系统。',
      rules: { validate: (value) => String(value ?? '').trim() ? true : '请选择所属系统。' },
      render: ({ field, controlProps }) => <PermissionSystemSelect
        {...controlProps}
        value={String(field.value ?? '')}
        onChange={field.onChange}
        onBlur={field.onBlur}
        inputRef={field.ref}
      />,
    },
    {
      name: 'code', label: '字典编码', required: true, readOnly: Boolean(dictionary),
      description: '同一系统内唯一，创建后不能修改。',
      inputProps: { maxLength: 100, autoComplete: 'off' },
      rules: { validate: (value) => validateDictionaryText(String(value), '字典编码') || true },
    },
    {
      name: 'name', label: '字典名称', required: true,
      inputProps: { maxLength: 100, autoComplete: 'off' },
      rules: { validate: (value) => validateDictionaryText(String(value), '字典名称') || true },
    },
    {
      name: 'enabled', label: '字典状态', kind: 'select', required: true,
      options: dictionaryEnabledOptions, selectProps: { clearable: false },
      description: '停用字典后不可用于业务取值，不改变各字典项的启停设置。',
      rules: { validate: (value) => value === 'true' || value === 'false' || '请选择字典状态。' },
    },
    {
      name: 'description', label: '字典说明', kind: 'textarea',
      textareaProps: { rows: 4, maxLength: 500 },
      rules: { maxLength: { value: 500, message: '字典说明最多 500 个字符。' } },
    },
  ]

  function mapError(cause: unknown): FormIssue<IDictionaryFormValues>[] {
    const message = dictionaryError(cause, dictionary ? 'edit' : 'create')
    if (!dictionary && isAxiosError(cause)) {
      if (cause.response?.data?.code === 'DICTIONARY_SYSTEM_NOT_FOUND') return [{ name: 'systemCode', message }]
      if (cause.response?.status === 409) return [{ name: 'code', message }]
    }
    return [{ message }]
  }

  return (
    <Dialog open onOpenChange={(open) => { if (!open && !busy) onClose() }}>
      <DialogContent className="flex h-[82vh] max-h-[760px] w-[calc(100vw-48px)] max-w-[1280px] flex-col gap-5 overflow-hidden bg-card" aria-describedby={undefined} showCloseButton={!busy}>
        <DialogHeader className="shrink-0 border-b pb-4"><DialogTitle>{dictionary ? '编辑字典' : '新增字典'}</DialogTitle></DialogHeader>
        <Form
          form={form}
          fields={fields.map((field) => ({ ...field, disabled: busy }))}
          aria-label={dictionary ? '编辑字典' : '新增字典'}
          className="flex min-h-0 flex-1 flex-col space-y-0 gap-4"
          fieldsClassName="min-h-0 flex-1 space-y-0 overflow-hidden"
          renderFields={(items) => (
            <div className="grid h-full min-h-0 grid-cols-[minmax(260px,0.85fr)_minmax(0,2fr)] gap-5">
              <section aria-label="基本信息" className="min-w-0 space-y-4 overflow-y-auto overscroll-contain py-1 pr-4">
                <h2 className="text-sm font-semibold">基本信息</h2>
                {dictionary && <dl className="grid grid-cols-[90px_minmax(0,1fr)] gap-3 text-sm">
                  <dt className="text-muted-foreground">所属系统</dt><dd className="break-all">{dictionary.systemName}</dd>
                  <dt className="text-muted-foreground">系统编码</dt><dd className="break-all font-mono">{dictionary.systemCode}</dd>
                </dl>}
                {items.map((item) => item.node)}
              </section>
              <DictionaryItemsEditor form={form} disabled={busy} originalCodes={dictionary?.items.map((item) => item.code) ?? []} />
            </div>
          )}
          mapError={mapError}
          onSubmit={async (values) => {
            const payload = {
              name: values.name.trim(),
              description: values.description.trim(),
              enabled: values.enabled === 'true',
              items: values.items.map((item) => ({
                ...(item.itemId ? { id: item.itemId } : {}),
                code: item.code.trim(),
                name: item.name.trim(),
                sortOrder: Number(item.sortOrder),
                isDefault: item.isDefault,
                enabled: item.enabled,
              })),
            }
            if (dictionary) {
              await editDictionary(dictionary.id, { ...payload, version: dictionary.version })
            } else {
              await createDictionary({ ...payload, code: values.code.trim(), systemCode: values.systemCode.trim() })
            }
            onSuccess(dictionary ? '字典及字典项已整体更新。' : '字典及字典项已创建。')
          }}
        >
          <div className="flex shrink-0 gap-3 border-t pt-4">
            <Button type="submit" disabled={busy}>{busy ? '正在保存…' : dictionary ? '保存修改' : '创建字典'}</Button>
            <Button type="button" variant="outline" disabled={busy} onClick={onClose}>取消</Button>
          </div>
        </Form>
      </DialogContent>
    </Dialog>
  )
}
