import { isAxiosError } from 'axios'
import { useState } from 'react'
import { createUlid } from '@/lib/ulid'
import { useForm, useWatch } from 'react-hook-form'
import { Button } from '@/components/ui/button'
import { Dialog, DialogContent, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { Form, type FormFieldConfig, type FormIssue } from '@/components/ui/form'
import {
  createManagedSystem, editManagedSystem, systemStatusOptions,
  type IManagedSystem,
} from '@/services/managed-systems'
import {
  isSystemStatus, managedSystemError, validateSystemEntry,
  validateSystemSort, validateSystemText,
} from '@/lib/managed-system-validation'

type SystemFormValues = {
  name: string
  description: string
  entryUrl: string
  status: string
  /* 数字输入在表单中保留字符串，提交时显式转换。 */
  sortOrder: string
}

interface IManagedSystemFormDialogProps {
  system: IManagedSystem | null
  onClose: () => void
  onSuccess: (message: string) => void
}

/** 新增与编辑共用字段配置；编辑记录由详情接口提供。 */
export function ManagedSystemFormDialog({ system, onClose, onSuccess }: IManagedSystemFormDialogProps) {
  // 每次挂载新增弹窗生成一次，重渲染和保存失败时保留当前编码。
  const [initialCode] = useState(() => system?.code ?? createUlid())
  const form = useForm<SystemFormValues>({
    mode: 'onBlur',
    reValidateMode: 'onChange',
    defaultValues: {
      name: system?.name ?? '',
      description: system?.description ?? '',
      entryUrl: system?.entryUrl ?? '',
      status: system?.status ?? 'NOT_LAUNCHED',
      sortOrder: String(system?.sortOrder ?? 0),
    },
  })
  const status = useWatch({ control: form.control, name: 'status' })
  const saving = form.formState.isSubmitting
  const busy = saving || form.formState.isValidating
  const fields: FormFieldConfig<SystemFormValues>[] = [
    {
      name: 'name', label: '系统名称', required: true,
      description: system ? '修改名称后，会同步相关权限、岗位及岗位权限中的系统名称。已打开的编辑窗口需要重新读取。' : undefined,
      inputProps: { maxLength: 100, autoComplete: 'off' },
      rules: { validate: (value) => validateSystemText(String(value), '系统名称') || true },
    },
    {
      name: 'status', label: '系统状态', kind: 'select', required: true,
      options: systemStatusOptions, selectProps: { clearable: false },
      description: '人工维护的业务状态，不代表实时健康检查或用户权限。',
      rules: { validate: (value) => isSystemStatus(String(value)) || '请选择有效的系统状态。' },
    },
    {
      name: 'entryUrl', label: '入口地址', required: status === 'NORMAL',
      dependencies: ['status'],
      inputProps: { type: 'url', maxLength: 2048, placeholder: 'https://example.com', autoComplete: 'off' },
      description: 'HTTP/HTTPS 完整地址，不能包含账号密码；正常状态必填。',
      rules: { validate: (value, values) => validateSystemEntry(String(value), values.status) || true },
    },
    {
      name: 'sortOrder', label: '排序', required: true,
      inputProps: { type: 'number', min: 0, max: 2147483647, step: 1 },
      description: '非负整数，数值越小越靠前。',
      rules: { validate: (value) => validateSystemSort(String(value)) || true },
    },
    {
      name: 'description', label: '系统说明', kind: 'textarea',
      textareaProps: { rows: 3, maxLength: 512 },
      rules: { maxLength: { value: 512, message: '系统说明最多 512 个字符。' } },
    },
  ]

  function mapError(cause: unknown): FormIssue<SystemFormValues>[] {
    const message = managedSystemError(cause, system ? 'edit' : 'create')
    if (isAxiosError(cause)) {
      if (!system && cause.response?.status === 409) {
        return [{ message: '系统编码冲突，请关闭后重新打开新增弹窗生成新编码。' }]
      }
      const code = cause.response?.data?.code
      if (code === 'SYSTEM_ENTRY_REQUIRED' || code === 'INVALID_SYSTEM_ENTRY_URL') {
        return [{ name: 'entryUrl', message }]
      }
      if (code === 'INVALID_SYSTEM_STATUS') return [{ name: 'status', message }]
      if (code === 'INVALID_SYSTEM_SORT') return [{ name: 'sortOrder', message }]
    }
    return [{ message }]
  }

  return (
    <Dialog open onOpenChange={(open) => { if (!open && !busy) onClose() }}>
      <DialogContent aria-describedby={undefined} showCloseButton={!busy}>
        <DialogHeader>
          <DialogTitle>{system ? '编辑系统' : '新增系统'}</DialogTitle>
        </DialogHeader>
        <dl className="space-y-2 text-sm">
          <dt className="font-medium">系统编码</dt>
          <dd className="break-all font-mono select-text">{initialCode}</dd>
        </dl>
        <Form
          form={form}
          fields={fields.map((field) => ({ ...field, disabled: busy }))}
          aria-label={system ? '编辑系统' : '新增系统'}
          mapError={mapError}
          onSubmit={async (values) => {
            if (!isSystemStatus(values.status)) throw new Error('无效系统状态')
            const payload = {
              name: values.name.trim(),
              description: values.description.trim(),
              entryUrl: values.entryUrl.trim(),
              status: values.status,
              sortOrder: Number(values.sortOrder),
            }
            if (system) {
              await editManagedSystem(system.id, { ...payload, version: system.version })
            } else {
              await createManagedSystem({ ...payload, code: initialCode })
            }
            onSuccess(system ? '系统资料已更新。' : '系统已创建。')
          }}
        >
          <div className="flex gap-3">
            <Button type="submit" disabled={busy}>
              {saving ? '正在保存…' : system ? '保存修改' : '创建系统'}
            </Button>
            <Button type="button" variant="outline" disabled={busy} onClick={onClose}>取消</Button>
          </div>
        </Form>
      </DialogContent>
    </Dialog>
  )
}
