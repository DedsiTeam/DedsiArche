import { useForm } from 'react-hook-form'
import { Form, type FormFieldConfig, type FormIssue } from '@/components/ui/form'
import { Button } from '@/components/ui/button'
import { Dialog, DialogContent, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { PermissionSystemSelect } from '@/components/PermissionSystemSelect'
import { PositionPermissionSelect } from '@/components/PositionPermissionSelect'
import { createPosition, editPosition, positionEnabledOptions, type IPosition } from '@/services/positions'
import { positionError, validatePositionSort, validatePositionText } from '@/lib/position-validation'

type PositionFormValues = {
  code: string
  systemCode: string
  name: string
  description: string
  enabled: string
  sortOrder: string
  permissionIds: string[]
}

interface IPositionFormDialogProps {
  position: IPosition | null
  onClose: () => void
  onSuccess: (message: string) => void
}

/** 编辑仅接收完整聚合详情，岗位字段与全部已选权限一起保存。 */
export function PositionFormDialog({ position, onClose, onSuccess }: IPositionFormDialogProps) {
  const form = useForm<PositionFormValues>({
    mode: 'onBlur',
    reValidateMode: 'onChange',
    defaultValues: {
      code: position?.code ?? '',
      systemCode: position?.systemCode ?? '',
      name: position?.name ?? '',
      description: position?.description ?? '',
      enabled: String(position?.enabled ?? true),
      sortOrder: String(position?.sortOrder ?? 0),
      permissionIds: position?.permissions.map((permission) => permission.permissionId) ?? [],
    },
  })
  const busy = form.formState.isSubmitting || form.formState.isValidating
  const fields: FormFieldConfig<PositionFormValues>[] = [
    {
      name: 'systemCode', label: '所属系统', required: true,
      visible: () => !position?.systemCode,
      description: position ? '此岗位尚未设置所属系统，请先选择；保存后不能更改。' : '创建后不能更改所属系统。',
      rules: { validate: (value) => String(value ?? '').trim() ? true : '请选择所属系统。' },
      render: ({ field, controlProps }) => (
        <PermissionSystemSelect
          {...controlProps}
          value={String(field.value ?? '')}
          onChange={field.onChange}
          onBlur={field.onBlur}
          inputRef={field.ref}
        />
      ),
    },
    {
      name: 'code', label: '岗位编码', required: true, readOnly: Boolean(position),
      description: '岗位编码全局唯一，创建后不能修改。',
      inputProps: { maxLength: 100, autoComplete: 'off' },
      rules: { validate: (value) => validatePositionText(String(value), '岗位编码') || true },
    },
    {
      name: 'name', label: '岗位名称', required: true,
      inputProps: { maxLength: 100, autoComplete: 'off' },
      rules: { validate: (value) => validatePositionText(String(value), '岗位名称') || true },
    },
    {
      name: 'enabled', label: '岗位状态', kind: 'select', required: true,
      options: positionEnabledOptions, selectProps: { clearable: false },
      rules: { validate: (value) => value === 'true' || value === 'false' || '请选择岗位状态。' },
    },
    {
      name: 'sortOrder', label: '排序', required: true,
      inputProps: { type: 'number', min: 0, max: 2147483647, step: 1 },
      description: '非负整数，数值越小越靠前。',
      rules: { validate: (value) => validatePositionSort(String(value)) || true },
    },
    {
      name: 'description', label: '岗位说明', kind: 'textarea',
      textareaProps: { rows: 3, maxLength: 500 },
      rules: { maxLength: { value: 500, message: '岗位说明最多 500 个字符。' } },
    },
    {
      name: 'permissionIds', label: '岗位权限',
      className: 'flex min-h-0 flex-1 flex-col space-y-0 gap-3 [&>label]:font-semibold',
      render: ({ field, controlProps }) => (
        <PositionPermissionSelect
          {...controlProps}
          value={field.value as string[]}
          onChange={field.onChange}
          onBlur={field.onBlur}
          inputRef={field.ref}
          initialPermissions={position?.permissions ?? []}
        />
      ),
    },
  ]

  function mapError(cause: unknown): FormIssue<PositionFormValues>[] {
    // 409 也可能是权限引用并发变化，保留整单输入并统一提示重读。
    return [{ message: positionError(cause, position ? 'edit' : 'create') }]
  }

  return (
    <Dialog open onOpenChange={(open) => { if (!open && !busy) onClose() }}>
      <DialogContent className="flex h-[82vh] max-h-[760px] w-[calc(100vw-48px)] max-w-[1280px] flex-col gap-5 overflow-hidden bg-card" aria-describedby={undefined} showCloseButton={!busy}>
        <DialogHeader className="shrink-0 border-b pb-4"><DialogTitle>{position ? '编辑岗位' : '新增岗位'}</DialogTitle></DialogHeader>
        <Form
          form={form}
          fields={fields.map((field) => ({ ...field, disabled: busy }))}
          aria-label={position ? '编辑岗位' : '新增岗位'}
          className="flex min-h-0 flex-1 flex-col space-y-0 gap-4"
          fieldsClassName="min-h-0 flex-1 space-y-0 overflow-hidden"
          renderFields={(items) => (
            <div className="grid h-full min-h-0 grid-cols-[minmax(260px,0.85fr)_minmax(0,2fr)] gap-5">
              <section aria-label="基本信息" className="min-w-0 space-y-4 overflow-y-auto overscroll-contain py-1 pr-4">
                <h2 className="text-sm font-semibold">基本信息</h2>
                {position?.systemCode && (
                  <dl className="grid grid-cols-[100px_minmax(0,1fr)] gap-3 text-sm">
                    <dt className="text-muted-foreground">所属系统</dt>
                    <dd className="break-all">{position.systemName ?? '未设置'}</dd>
                    <dt className="text-muted-foreground">系统编码</dt>
                    <dd className="break-all font-mono">{position.systemCode}</dd>
                  </dl>
                )}
                <div className="grid grid-cols-2 gap-x-3 gap-y-4">
                  {items.filter((item) => item.name !== 'permissionIds').map((item) => (
                    <div key={item.name} className={item.name === 'enabled' || item.name === 'sortOrder' ? 'min-w-0' : 'col-span-2 min-w-0'}>
                      {item.node}
                    </div>
                  ))}
                </div>
              </section>
              <section aria-label="岗位权限" className="flex min-h-0 min-w-0 flex-col border-l py-1 pl-5">
                {items.filter((item) => item.name === 'permissionIds').map((item) => item.node)}
              </section>
            </div>
          )}
          mapError={mapError}
          onSubmit={async (values) => {
            const payload = {
              name: values.name.trim(),
              description: values.description.trim(),
              enabled: values.enabled === 'true',
              sortOrder: Number(values.sortOrder),
              // 不从当前搜索页计算集合；空数组明确表示清空全部权限。
              permissionIds: [...values.permissionIds],
            }
            if (position) {
              await editPosition(position.id, {
                ...payload,
                version: position.version,
                // 已归属岗位不可换系统；只有旧岗位首次绑定时提交所属系统编码。
                ...(position.systemCode ? {} : { systemCode: values.systemCode.trim() }),
              })
            } else {
              await createPosition({
                ...payload,
                code: values.code.trim(),
                systemCode: values.systemCode.trim(),
              })
            }
            onSuccess(position ? '岗位及权限已整体更新。' : '岗位及权限已创建。')
          }}
        >
          <div className="flex shrink-0 gap-3 border-t pt-4">
            <Button type="submit" disabled={busy}>{busy ? '正在保存…' : position ? '保存修改' : '创建岗位'}</Button>
            <Button type="button" variant="outline" disabled={busy} onClick={onClose}>取消</Button>
          </div>
        </Form>
      </DialogContent>
    </Dialog>
  )
}
