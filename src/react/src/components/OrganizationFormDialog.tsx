import { useEffect, useState } from 'react'
import { useForm, useWatch } from 'react-hook-form'
import { isAxiosError } from 'axios'
import { Form, type FormFieldConfig, type FormIssue } from '@/components/ui/form'
import { Button } from '@/components/ui/button'
import { Select } from '@/components/ui/select'
import { Dialog, DialogContent, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { PermissionSystemSelect } from '@/components/PermissionSystemSelect'
import {
  createOrganization, editOrganization, organizationTree, organizationEnabledOptions,
  type IOrganization, type IOrganizationNode,
} from '@/services/organizations'
import { organizationError, organizationParentOptions, validateOrganizationSort, validateOrganizationText } from '@/lib/organization-validation'

type OrganizationFormValues = {
  systemCode: string
  parentId: string
  code: string
  name: string
  name1: string
  name2: string
  name3: string
  name4: string
  sortOrder: string
  description: string
  enabled: string
}

interface IOrganizationFormDialogProps {
  organization: IOrganization | null
  initialSystemCode?: string
  onClose: () => void
  onSuccess: (message: string) => void
}

/** 上级选择来源于完整同系统树；parentId 在每次提交时都显式发送。 */
export function OrganizationFormDialog({ organization, initialSystemCode = '', onClose, onSuccess }: IOrganizationFormDialogProps) {
  const form = useForm<OrganizationFormValues>({
    mode: 'onBlur',
    reValidateMode: 'onChange',
    defaultValues: {
      systemCode: organization?.systemCode ?? initialSystemCode,
      parentId: organization?.parentId ?? '',
      code: organization?.code ?? '',
      name: organization?.name ?? '',
      name1: organization?.name1 ?? '',
      name2: organization?.name2 ?? '',
      name3: organization?.name3 ?? '',
      name4: organization?.name4 ?? '',
      sortOrder: String(organization?.sortOrder ?? 0),
      description: organization?.description ?? '',
      enabled: String(organization?.enabled ?? true),
    },
  })
  // 保持订阅，同时从表单读取最新快照；字段注册早于 watch 订阅时也保留预填值。
  useWatch({ control: form.control, name: ['systemCode', 'enabled'] })
  const enabled = form.getValues('enabled') === 'true'
  const systemCode = organization?.systemCode ?? form.getValues('systemCode') ?? ''
  const [tree, setTree] = useState<IOrganizationNode[]>([])
  const [loadedSystem, setLoadedSystem] = useState('')
  const [loadingTree, setLoadingTree] = useState(false)
  const [treeError, setTreeError] = useState('')
  const [attempt, setAttempt] = useState(0)
  const busy = form.formState.isSubmitting || form.formState.isValidating

  useEffect(() => {
    const controller = new AbortController()
    setTree([])
    setTreeError('')
    if (!systemCode) {
      setLoadedSystem('')
      setLoadingTree(false)
      return () => controller.abort()
    }
    setLoadingTree(true)
    organizationTree(systemCode, controller.signal)
      .then((result) => {
        if (controller.signal.aborted) return
        setTree(result)
        setLoadedSystem(systemCode)
      })
      .catch((cause: unknown) => { if (!controller.signal.aborted) setTreeError(organizationError(cause)) })
      .finally(() => { if (!controller.signal.aborted) setLoadingTree(false) })
    return () => controller.abort()
  }, [systemCode, attempt])

  const waitingTree = Boolean(systemCode) && (loadingTree || loadedSystem !== systemCode)
  const parentOptions = organizationParentOptions(loadedSystem === systemCode ? tree : [], organization?.id, enabled)
  const fields: FormFieldConfig<OrganizationFormValues>[] = [
    {
      name: 'systemCode', label: '所属系统', required: true, visible: () => !organization,
      description: '创建后不能更改所属系统。',
      rules: { validate: (value) => String(value ?? '').trim() ? true : '请选择所属系统。' },
      render: ({ field, controlProps }) => (
        <PermissionSystemSelect
          {...controlProps}
          value={String(field.value ?? '')}
          onBlur={field.onBlur}
          inputRef={field.ref}
          onChange={(value) => {
            field.onChange(value)
            // 换系统时显式回到根级，不把另一系统上级带入请求。
            form.setValue('parentId', '', { shouldDirty: true, shouldValidate: true })
          }}
        />
      ),
    },
    {
      name: 'parentId', label: '上级组织', dependencies: ['systemCode', 'enabled'],
      description: '根组织无上级；不能选择自身或下级。启用组织只能选择启用的上级。',
      rules: { validate: (value) => {
        if (!value) return true
        if (waitingTree || treeError) return '请先成功加载上级组织。'
        const option = parentOptions.find((item) => item.value === value)
        return option ? (!option.disabled || '启用组织的上级必须启用。') : '上级组织不可用，请重新选择。'
      } },
      render: ({ field, controlProps }) => <div className="space-y-2">
        <Select
          {...controlProps}
          ref={field.ref}
          onBlur={field.onBlur}
          className="w-full"
          value={field.value ? String(field.value) : '__root__'}
          clearValue="__root__"
          clearLabel="设为根组织"
          disabled={busy || !systemCode || waitingTree || Boolean(treeError)}
          options={[{ value: '__root__', label: '无上级（根组织）' }, ...parentOptions]}
          onValueChange={(value) => field.onChange(value === '__root__' ? '' : value)} />
        {waitingTree && !treeError && <p role="status" className="text-xs text-muted-foreground">正在加载上级组织…</p>}
        {treeError && <p role="alert" className="text-sm text-error">{treeError}<Button type="button" variant="link" disabled={busy} onClick={() => setAttempt((value) => value + 1)}>重新加载</Button></p>}
      </div>,
    },
    {
      name: 'code', label: '组织编码', required: true, readOnly: Boolean(organization),
      description: '同一系统内唯一，创建后不能修改。', inputProps: { maxLength: 100, autoComplete: 'off' },
      rules: { validate: (value) => validateOrganizationText(String(value), '组织编码') || true },
    },
    {
      name: 'name', label: '组织名称', required: true, inputProps: { maxLength: 100, autoComplete: 'off' },
      rules: { validate: (value) => validateOrganizationText(String(value), '组织名称') || true },
    },
    {
      name: 'enabled', label: '组织状态', kind: 'select', required: true,
      options: organizationEnabledOptions, selectProps: { clearable: false },
      description: '存在启用的下级组织时，需先处理下级才能停用。',
      rules: { validate: (value) => value === 'true' || value === 'false' || '请选择组织状态。' },
    },
    {
      name: 'sortOrder', label: '排序', required: true,
      inputProps: { type: 'number', min: 0, max: 2147483647, step: 1 },
      rules: { validate: (value) => validateOrganizationSort(String(value)) || true },
    },
    ...(['name1', 'name2', 'name3', 'name4'] as const).map((name, index): FormFieldConfig<OrganizationFormValues> => ({
      name, label: `别名 ${index + 1}`, inputProps: { maxLength: 100, autoComplete: 'off' },
      rules: { maxLength: { value: 100, message: '组织别名最多 100 个字符。' } },
    })),
    {
      name: 'description', label: '组织说明', kind: 'textarea', textareaProps: { rows: 4, maxLength: 500 },
      rules: { maxLength: { value: 500, message: '组织说明最多 500 个字符。' } },
    },
  ]

  function mapError(cause: unknown): FormIssue<OrganizationFormValues>[] {
    const message = organizationError(cause, organization ? 'edit' : 'create')
    if (!organization && isAxiosError(cause)) {
      if (cause.response?.data?.code === 'ORGANIZATION_SYSTEM_NOT_FOUND') return [{ name: 'systemCode', message }]
      if (cause.response?.status === 409) return [{ name: 'code', message }]
    }
    return [{ message }]
  }

  const extensionNames = new Set(['name1', 'name2', 'name3', 'name4', 'description'])
  return (
    <Dialog open onOpenChange={(open) => { if (!open && !busy) onClose() }}>
      <DialogContent className="flex h-[82vh] max-h-[760px] w-[calc(100vw-48px)] max-w-[1100px] flex-col gap-5 overflow-hidden bg-card" aria-describedby={undefined} showCloseButton={!busy}>
        <DialogHeader className="shrink-0 border-b pb-4"><DialogTitle>{organization ? '编辑组织机构' : '新增组织机构'}</DialogTitle></DialogHeader>
        <Form form={form} fields={fields.map((field) => ({ ...field, disabled: busy }))} aria-label={organization ? '编辑组织机构' : '新增组织机构'}
          className="flex min-h-0 flex-1 flex-col space-y-0 gap-4" fieldsClassName="min-h-0 flex-1 space-y-0 overflow-hidden"
          renderFields={(items) => <div className="grid h-full min-h-0 grid-cols-2 gap-5">
            <section aria-label="基本信息" className="min-w-0 space-y-4 overflow-y-auto overscroll-contain pr-4 py-1">
              <h2 className="text-sm font-semibold">基本信息</h2>
              {organization && <dl className="grid grid-cols-[90px_minmax(0,1fr)] gap-3 text-sm">
                <dt className="text-muted-foreground">所属系统</dt><dd className="break-all">{organization.systemName}</dd>
                <dt className="text-muted-foreground">系统编码</dt><dd className="break-all font-mono">{organization.systemCode}</dd>
              </dl>}
              {items.filter((item) => !extensionNames.has(item.name)).map((item) => item.node)}
            </section>
            <section aria-label="扩展信息" className="min-w-0 space-y-4 overflow-y-auto overscroll-contain border-l pl-5 py-1">
              <h2 className="text-sm font-semibold">扩展信息</h2>
              {items.filter((item) => extensionNames.has(item.name)).map((item) => item.node)}
            </section>
          </div>}
          validate={() => waitingTree || treeError ? [{ message: '请先成功加载所属系统的组织树后保存。' }] : []}
          mapError={mapError}
          onSubmit={async (values) => {
            const payload = {
              parentId: values.parentId || null,
              name: values.name.trim(),
              name1: values.name1.trim(),
              name2: values.name2.trim(),
              name3: values.name3.trim(),
              name4: values.name4.trim(),
              sortOrder: Number(values.sortOrder),
              description: values.description.trim(),
              enabled: values.enabled === 'true',
            }
            if (organization) await editOrganization(organization.id, { ...payload, version: organization.version })
            else await createOrganization({ ...payload, systemCode: values.systemCode.trim(), code: values.code.trim() })
            onSuccess(organization ? '组织机构已更新。' : '组织机构已创建。')
          }}>
          <div className="flex shrink-0 gap-3 border-t pt-4">
            <Button type="submit" disabled={busy || waitingTree || Boolean(treeError)}>{busy ? '正在保存…' : organization ? '保存修改' : '创建组织'}</Button>
            <Button type="button" variant="outline" disabled={busy} onClick={onClose}>取消</Button>
          </div>
        </Form>
      </DialogContent>
    </Dialog>
  )
}
