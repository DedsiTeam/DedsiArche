import { isAxiosError } from 'axios'
import { useForm } from 'react-hook-form'
import { Button } from '@/components/ui/button'
import { Dialog, DialogContent, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { Form, type FormFieldConfig, type FormIssue } from '@/components/ui/form'
import { PermissionSystemSelect } from '@/components/PermissionSystemSelect'
import { createPermission, editPermission, permissionEnabledOptions, type IPermission } from '@/services/permissions'
import { permissionError, validatePermissionSort, validatePermissionText } from '@/lib/permission-validation'

type PermissionFormValues = {
  systemCode: string
  systemName: string
  code: string
  name: string
  description: string
  enabled: string
  sortOrder: string
}

interface IPermissionFormDialogProps {
  permission: IPermission | null
  onClose: () => void
  onSuccess: (message: string) => void
}

/** 所属系统与权限编码只在创建时指定，系统名称始终由后端维护。 */
export function PermissionFormDialog({ permission, onClose, onSuccess }: IPermissionFormDialogProps) {
  const form = useForm<PermissionFormValues>({
    mode: 'onBlur',
    reValidateMode: 'onChange',
    defaultValues: {
      systemCode: permission?.systemCode ?? '',
      systemName: permission?.systemName ?? '',
      code: permission?.code ?? '',
      name: permission?.name ?? '',
      description: permission?.description ?? '',
      enabled: String(permission?.enabled ?? true),
      sortOrder: String(permission?.sortOrder ?? 0),
    },
  })
  const saving = form.formState.isSubmitting
  const busy = saving || form.formState.isValidating
  const fields: FormFieldConfig<PermissionFormValues>[] = [
    {
      name: 'systemCode', label: '所属系统', required: true, readOnly: Boolean(permission),
      description: '创建后不能修改所属系统。系统名称由服务端读取。',
      rules: { validate: (value) => String(value).trim() ? true : '请选择所属系统。' },
      ...(permission ? {} : {
        render: ({ field, controlProps }) => (
          <PermissionSystemSelect
            {...controlProps}
            value={String(field.value ?? '')}
            onChange={field.onChange}
            onBlur={field.onBlur}
            inputRef={field.ref}
          />
        ),
      } satisfies Partial<FormFieldConfig<PermissionFormValues>>),
    },
    {
      name: 'systemName', label: '系统名称', readOnly: true,
      visible: () => Boolean(permission),
    },
    {
      name: 'code', label: '权限编码', required: true, readOnly: Boolean(permission),
      description: '同一系统内唯一，区分大小写，创建后不能修改。',
      inputProps: { maxLength: 100, autoComplete: 'off' },
      rules: { validate: (value) => validatePermissionText(String(value), '权限编码') || true },
    },
    {
      name: 'name', label: '权限名称', required: true,
      description: permission ? '修改名称会同步岗位权限与菜单权限快照，并更新相关版本；已打开的编辑窗口需要重新读取。' : undefined,
      inputProps: { maxLength: 100, autoComplete: 'off' },
      rules: { validate: (value) => validatePermissionText(String(value), '权限名称') || true },
    },
    {
      name: 'enabled', label: '启用状态', kind: 'select', required: true,
      options: permissionEnabledOptions,
      selectProps: { clearable: false },
      description: '控制权限定义的启停，当前不直接承担用户授权判定。',
      rules: { validate: (value) => value === 'true' || value === 'false' || '请选择启用状态。' },
    },
    {
      name: 'sortOrder', label: '排序', required: true,
      description: '非负整数，数值越小越靠前。',
      inputProps: { type: 'number', min: 0, max: 2147483647, step: 1 },
      rules: { validate: (value) => validatePermissionSort(String(value)) || true },
    },
    {
      name: 'description', label: '权限说明', kind: 'textarea',
      textareaProps: { rows: 3, maxLength: 500 },
      rules: { maxLength: { value: 500, message: '权限说明最多 500 个字符。' } },
    },
  ]

  function mapError(cause: unknown): FormIssue<PermissionFormValues>[] {
    const message = permissionError(cause, permission ? 'edit' : 'create')
    if (!permission && isAxiosError(cause) && cause.response?.status === 409) {
      return [{ name: 'code', message }]
    }
    return [{ message }]
  }

  return (
    <Dialog open onOpenChange={(open) => { if (!open && !busy) onClose() }}>
      <DialogContent aria-describedby={undefined} showCloseButton={!busy}>
        <DialogHeader><DialogTitle>{permission ? '编辑权限' : '新增权限'}</DialogTitle></DialogHeader>
        <Form
          form={form}
          fields={fields.map((field) => ({ ...field, disabled: busy }))}
          aria-label={permission ? '编辑权限' : '新增权限'}
          mapError={mapError}
          onSubmit={async (values) => {
            // 显式组装请求，不发送只读系统名称和不可变字段。
            const payload = {
              name: values.name.trim(),
              description: values.description.trim(),
              enabled: values.enabled === 'true',
              sortOrder: Number(values.sortOrder),
            }
            if (permission) {
              await editPermission(permission.id, { ...payload, version: permission.version })
            } else {
              await createPermission({ ...payload, systemCode: values.systemCode, code: values.code.trim() })
            }
            onSuccess(permission ? '权限资料已更新。' : '权限已创建。')
          }}
        >
          <div className="flex gap-3">
            <Button type="submit" disabled={busy}>{saving ? '正在保存…' : permission ? '保存修改' : '创建权限'}</Button>
            <Button type="button" variant="outline" disabled={busy} onClick={onClose}>取消</Button>
          </div>
        </Form>
      </DialogContent>
    </Dialog>
  )
}
