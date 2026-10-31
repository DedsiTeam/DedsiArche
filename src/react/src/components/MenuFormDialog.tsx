import { useEffect, useState } from 'react'
import { useForm, useWatch } from 'react-hook-form'
import { isAxiosError } from 'axios'
import { Form, type FormFieldConfig, type FormIssue } from '@/components/ui/form'
import { Button } from '@/components/ui/button'
import { Select } from '@/components/ui/select'
import { Dialog, DialogContent, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { MenuPermissionSelect } from '@/components/MenuPermissionSelect'
import { PermissionSystemSelect } from '@/components/PermissionSystemSelect'
import {
  createMenu, editMenu, menuTree, menuEnabledOptions, menuTypeOptions,
  type IMenu, type IMenuNode, type MenuType,
} from '@/services/menus'
import { menuError, menuParentOptions, menuLevel, validateMenuSort, validateMenuText, validateMenuRoute, validateMenuComponent } from '@/lib/menu-validation'

type MenuFormValues = {
  systemCode: string
  parentId: string
  code: string
  name: string
  menuType: MenuType
  permissionId: string
  routePath: string
  componentPath: string
  visible: boolean
  cache: boolean
  affix: boolean
  sortOrder: string
  description: string
  enabled: string
}

interface IMenuFormDialogProps {
  menu: IMenu | null
  initialSystemCode?: string
  onClose: () => void
  onSuccess: (message: string) => void
}

/** 上级选择来源于完整同系统树；parentId 在每次提交时都显式发送。 */
export function MenuFormDialog({ menu, initialSystemCode = '', onClose, onSuccess }: IMenuFormDialogProps) {
  const form = useForm<MenuFormValues>({
    mode: 'onBlur',
    reValidateMode: 'onChange',
    defaultValues: {
      systemCode: menu?.systemCode ?? initialSystemCode,
      parentId: menu?.parentId ?? '',
      code: menu?.code ?? '',
      name: menu?.name ?? '',
      menuType: menu?.menuType ?? 'PAGE',
      permissionId: menu?.permissionId ?? '',
      routePath: menu?.routePath ?? '',
      componentPath: menu?.componentPath ?? '',
      visible: menu?.visible ?? true,
      cache: menu?.cache ?? true,
      affix: menu?.affix ?? false,
      sortOrder: String(menu?.sortOrder ?? 0),
      description: menu?.description ?? '',
      enabled: String(menu?.enabled ?? true),
    },
  })
  // 保持订阅，同时从表单读取最新快照；字段注册早于 watch 订阅时也保留预填值。
  useWatch({ control: form.control, name: ['systemCode', 'enabled', 'menuType', 'parentId'] })
  const menuType = form.getValues('menuType')
  const parentId = form.getValues('parentId')
  const enabled = form.getValues('enabled') === 'true'
  const systemCode = menu?.systemCode ?? form.getValues('systemCode') ?? ''
  const [tree, setTree] = useState<IMenuNode[]>([])
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
    menuTree(systemCode, controller.signal)
      .then((result) => {
        if (controller.signal.aborted) return
        setTree(result)
        setLoadedSystem(systemCode)
      })
      .catch((cause: unknown) => { if (!controller.signal.aborted) setTreeError(menuError(cause)) })
      .finally(() => { if (!controller.signal.aborted) setLoadingTree(false) })
    return () => controller.abort()
  }, [systemCode, attempt])

  const waitingTree = Boolean(systemCode) && (loadingTree || loadedSystem !== systemCode)
  const parentOptions = menuParentOptions(loadedSystem === systemCode ? tree : [], menu?.id, enabled)
  const fields: FormFieldConfig<MenuFormValues>[] = [
    {
      name: 'systemCode', label: '所属系统', required: true, visible: () => !menu,
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
            form.setValue('permissionId', '', { shouldDirty: true, shouldValidate: true })
          }}
        />
      ),
    },
    {
      name: 'parentId', label: '上级菜单', dependencies: ['systemCode', 'enabled'],
      description: '上级只能选同系统目录；不能选择自身或下级。启用菜单只能选择启用的上级。',
      rules: { validate: (value) => {
        if (!value) return true
        if (waitingTree || treeError) return '请先成功加载上级菜单。'
        const option = parentOptions.find((item) => item.value === value)
        return option ? (!option.disabled || '启用菜单的上级必须启用。') : '上级菜单不可用，请重新选择。'
      } },
      render: ({ field, controlProps }) => <div className="space-y-2">
        <Select
          {...controlProps}
          ref={field.ref}
          onBlur={field.onBlur}
          className="w-full"
          value={field.value ? String(field.value) : '__root__'}
          clearValue="__root__"
          clearLabel="设为根菜单"
          disabled={busy || !systemCode || waitingTree || Boolean(treeError)}
          options={[{ value: '__root__', label: '无上级（根菜单）' }, ...parentOptions]}
          onValueChange={(value) => field.onChange(value === '__root__' ? '' : value)} />
        {waitingTree && !treeError && <p role="status" className="text-xs text-muted-foreground">正在加载上级菜单…</p>}
        {treeError && <p role="alert" className="text-sm text-error">{treeError}<Button type="button" variant="link" disabled={busy} onClick={() => setAttempt((value) => value + 1)}>重新加载</Button></p>}
      </div>,
    },
    {
      name: 'code', label: '菜单编码', required: true, readOnly: Boolean(menu),
      description: '同一系统内唯一，创建后不能修改。', inputProps: { maxLength: 100, autoComplete: 'off' },
      rules: { validate: (value) => validateMenuText(String(value), '菜单编码') || true },
    },
    {
      name: 'name', label: '菜单名称', required: true, inputProps: { maxLength: 100, autoComplete: 'off' },
      rules: { validate: (value) => validateMenuText(String(value), '菜单名称') || true },
    },
    {
      name: 'enabled', label: '菜单状态', kind: 'select', required: true,
      options: menuEnabledOptions, selectProps: { clearable: false },
      description: '存在启用的下级菜单时，需先处理下级才能停用。',
      rules: { validate: (value) => value === 'true' || value === 'false' || '请选择菜单状态。' },
    },
    {
      name: 'sortOrder', label: '排序', required: true,
      inputProps: { type: 'number', min: 0, max: 2147483647, step: 1 },
      rules: { validate: (value) => validateMenuSort(String(value)) || true },
    },
    {
      name: 'menuType', label: '菜单类型', required: true,
      rules: { validate: (value) => ['DIRECTORY', 'PAGE', 'EXTERNAL'].includes(String(value)) || '请选择菜单类型。' },
      render: ({ field, controlProps }) => (
        <Select
          {...controlProps}
          ref={field.ref}
          onBlur={field.onBlur}
          className="w-full"
          clearable={false}
          value={String(field.value)}
          options={menuTypeOptions}
          onValueChange={(value) => {
            field.onChange(value)
            // 切换类型后清除上一类型的地址与行为，避免隐藏字段残留。
            form.setValue('routePath', '', { shouldDirty: true })
            form.setValue('componentPath', '', { shouldDirty: true })
            form.setValue('cache', value === 'PAGE', { shouldDirty: true })
            form.setValue('affix', false, { shouldDirty: true })
            form.clearErrors(['routePath', 'componentPath', 'cache', 'affix'])
          }}
        />
      ),
    },
    {
      name: 'routePath', label: menuType === 'EXTERNAL' ? '外链地址' : '页面路由',
      required: true, visible: () => menuType !== 'DIRECTORY', dependencies: ['menuType'],
      description: menuType === 'EXTERNAL' ? '完整 HTTP/HTTPS 地址，不含账号密码。' : '以 / 开头的站内路由，同系统内唯一，不含查询参数或片段。',
      inputProps: { maxLength: menuType === 'PAGE' ? 500 : 2048, autoComplete: 'off' },
      rules: { validate: (value, values) => validateMenuRoute(String(value ?? ''), values.menuType) || true },
    },
    {
      name: 'componentPath', label: '组件路径', required: true, visible: () => menuType === 'PAGE',
      description: '页面组件逻辑路径，不填写 URL 或相对穿越路径。',
      inputProps: { maxLength: 255, autoComplete: 'off' },
      rules: { validate: (value) => validateMenuComponent(String(value ?? '')) || true },
    },
    { name: 'visible', label: '显示菜单', kind: 'switch' },
    { name: 'cache', label: '缓存页面', kind: 'switch', visible: () => menuType === 'PAGE' },
    { name: 'affix', label: '固定页签', kind: 'switch', visible: () => menuType === 'PAGE' },
    {
      name: 'permissionId', label: '绑定权限',
      description: '仅可绑定同系统权限，可清空；停用权限仍允许配置。本页面不改变运行时鉴权。',
      render: ({ field, controlProps }) => (
        <MenuPermissionSelect
          key={systemCode}
          {...controlProps}
          inputRef={field.ref}
          onBlur={field.onBlur}
          systemCode={systemCode}
          value={String(field.value ?? '')}
          initialLabel={menu && menu.permissionId === field.value ? `${menu.permissionCode} · ${menu.permissionName}` : undefined}
          onChange={field.onChange}
        />
      ),
    },
    {
      name: 'description', label: '菜单说明', kind: 'textarea', textareaProps: { rows: 4, maxLength: 500 },
      rules: { maxLength: { value: 500, message: '菜单说明最多 500 个字符。' } },
    },
  ]

  function mapError(cause: unknown): FormIssue<MenuFormValues>[] {
    const message = menuError(cause, menu ? 'edit' : 'create')
    if (isAxiosError(cause)) {
      const code = cause.response?.data?.code
      const detail = cause.response?.data?.detail
      if (!menu && code === 'MENU_SYSTEM_NOT_FOUND') return [{ name: 'systemCode', message }]
      if (detail === '该系统下菜单编码已存在') return [{ name: 'code', message }]
      if (detail === '该系统下页面路径已存在') return [{ name: 'routePath', message }]
      if (detail === '存在下级菜单，不能改为非目录类型') return [{ name: 'menuType', message }]
      if (detail === '存在启用的下级菜单，不能停用') return [{ name: 'enabled', message }]
      const businessMessage = cause.response?.data?.message
      if (code === 'INVALID_MENU' && (businessMessage === '菜单权限必须属于同一系统' || businessMessage === '所选权限不存在')) return [{ name: 'permissionId', message }]
    }
    return [{ message }]
  }

  const extensionNames = new Set(['routePath', 'componentPath', 'visible', 'cache', 'affix', 'permissionId', 'description'])
  const parentLevel = parentId ? menuLevel(tree, parentId) : 0
  const level = parentLevel === undefined ? '待选择有效上级' : parentLevel + 1
  return (
    <Dialog open onOpenChange={(open) => { if (!open && !busy) onClose() }}>
      <DialogContent className="flex h-[82vh] max-h-[760px] w-[calc(100vw-48px)] max-w-[1100px] flex-col gap-5 overflow-hidden bg-card" aria-describedby={undefined} showCloseButton={!busy}>
        <DialogHeader className="shrink-0 border-b pb-4"><DialogTitle>{menu ? '编辑菜单' : '新增菜单'}</DialogTitle></DialogHeader>
        <Form form={form} fields={fields.map((field) => ({ ...field, disabled: busy }))} aria-label={menu ? '编辑菜单' : '新增菜单'}
          className="flex min-h-0 flex-1 flex-col space-y-0 gap-4" fieldsClassName="min-h-0 flex-1 space-y-0 overflow-hidden"
          renderFields={(items) => <div className="grid h-full min-h-0 grid-cols-2 gap-5">
            <section aria-label="基本信息" className="min-w-0 space-y-4 overflow-y-auto overscroll-contain pr-4 py-1">
              <h2 className="text-sm font-semibold">基本信息</h2>
              <p className="text-sm text-muted-foreground">菜单层级：{waitingTree ? '加载中…' : level}（由上级自动计算）</p>
              {menu && <dl className="grid grid-cols-[90px_minmax(0,1fr)] gap-3 text-sm">
                <dt className="text-muted-foreground">所属系统</dt><dd className="break-all">{menu.systemName}</dd>
                <dt className="text-muted-foreground">系统编码</dt><dd className="break-all font-mono">{menu.systemCode}</dd>
              </dl>}
              {items.filter((item) => !extensionNames.has(item.name)).map((item) => item.node)}
            </section>
            <section aria-label="路由与权限" className="min-w-0 space-y-4 overflow-y-auto overscroll-contain border-l pl-5 py-1">
              <h2 className="text-sm font-semibold">路由与权限</h2>
              {items.filter((item) => extensionNames.has(item.name)).map((item) => item.node)}
            </section>
          </div>}
          validate={() => waitingTree || treeError ? [{ message: '请先成功加载所属系统的菜单树后保存。' }] : []}
          mapError={mapError}
          onSubmit={async (values) => {
            const payload = {
              parentId: values.parentId || null,
              name: values.name.trim(),
              menuType: values.menuType,
              permissionId: values.permissionId || null,
              routePath: values.menuType === 'DIRECTORY' ? '' : values.routePath?.trim() ?? '',
              componentPath: values.menuType === 'PAGE' ? values.componentPath?.trim() ?? '' : '',
              visible: Boolean(values.visible),
              cache: values.menuType === 'PAGE' && Boolean(values.cache),
              affix: values.menuType === 'PAGE' && Boolean(values.affix),
              sortOrder: Number(values.sortOrder),
              description: values.description.trim(),
              enabled: values.enabled === 'true',
            }
            if (menu) await editMenu(menu.id, { ...payload, version: menu.version })
            else await createMenu({ ...payload, systemCode: values.systemCode.trim(), code: values.code.trim() })
            onSuccess(menu ? '菜单已更新。' : '菜单已创建。')
          }}>
          <div className="flex shrink-0 gap-3 border-t pt-4">
            <Button type="submit" disabled={busy || waitingTree || Boolean(treeError)}>{busy ? '正在保存…' : menu ? '保存修改' : '创建菜单'}</Button>
            <Button type="button" variant="outline" disabled={busy} onClick={onClose}>取消</Button>
          </div>
        </Form>
      </DialogContent>
    </Dialog>
  )
}
