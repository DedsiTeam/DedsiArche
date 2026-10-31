import { useEffect, useRef, useState } from 'react'
import { Button } from '@/components/ui/button'
import { Dialog, DialogContent, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { MenuFormDialog } from '@/components/MenuFormDialog'
import {
  deleteMenu, menuDetail, menuTree, menuTypeLabels,
  type IMenu,
} from '@/services/menus'
import { menuError, menuLevel } from '@/lib/menu-validation'
import { notify } from '@/lib/notify'

export type MenuAction = { kind: 'detail' | 'edit' | 'delete'; id: string }

interface IMenuActionDialogProps {
  action: MenuAction
  onClose: () => void
  onSuccess: (message: string) => void
}

/** 详情、编辑及删除均先读取最新记录；写入仍使用版本校验。 */
export function MenuActionDialog({ action, onClose, onSuccess }: IMenuActionDialogProps) {
  const [menu, setMenu] = useState<IMenu | null>(null)
  const [level, setLevel] = useState<number | undefined>(undefined)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [attempt, setAttempt] = useState(0)
  const [pending, setPending] = useState(false)
  const lock = useRef(false)
  const titles = { detail: '菜单详情', edit: '编辑菜单', delete: '确认删除菜单' }

  useEffect(() => {
    const controller = new AbortController()
    setMenu(null)
    setLoading(true)
    setError('')
    menuDetail(action.id, controller.signal)
      .then(async (result) => {
        if (action.kind === 'detail') {
          const tree = await menuTree(result.systemCode, controller.signal)
          if (controller.signal.aborted) return
          setLevel(menuLevel(tree, result.id))
        }
        if (!controller.signal.aborted) setMenu(result)
      })
      .catch((cause: unknown) => {
        if (!controller.signal.aborted) setError(menuError(cause))
      })
      .finally(() => { if (!controller.signal.aborted) setLoading(false) })
    return () => controller.abort()
  }, [action.id, action.kind, attempt])

  async function remove() {
    if (!menu || lock.current) return
    lock.current = true
    setPending(true)
    try {
      await deleteMenu(menu.id, menu.version)
      onSuccess(`菜单 ${menu.name} 已删除。`)
    } catch (cause) {
      notify.error(menuError(cause, 'delete'))
    } finally {
      lock.current = false
      setPending(false)
    }
  }

  if (!loading && !error && menu && action.kind === 'edit') {
    return <MenuFormDialog menu={menu} onClose={onClose} onSuccess={onSuccess} />
  }

  const details = menu ? [
    ['菜单 ID', menu.id],
    ['菜单编码', menu.code],
    ['菜单名称', menu.name],
    ['所属系统编码', menu.systemCode ?? '未设置'],
    ['所属系统名称', menu.systemName ?? '未设置'],
    ['菜单类型', menuTypeLabels[menu.menuType]],
    ['菜单状态', menu.enabled ? '已启用' : '已停用'],
    ['显示状态', menu.visible ? '显示' : '隐藏'],
    ['上级菜单 ID', menu.parentId || '无上级（根菜单）'],
    ['菜单层级', level === undefined ? '未找到，请刷新后重新读取' : String(level)],
    ['排序', String(menu.sortOrder)],
    ['创建时间', menu.createdAt.replace('T', ' ').slice(0, 19)],
    ['修改时间', menu.updatedAt.replace('T', ' ').slice(0, 19)],
  ] : []

  return (
    <Dialog open onOpenChange={(open) => { if (!open && !pending) onClose() }}>
      <DialogContent className={action.kind === 'detail'
        ? 'flex h-[82vh] max-h-[760px] w-[calc(100vw-48px)] max-w-[1100px] flex-col gap-5 overflow-hidden bg-card'
        : 'w-[calc(100vw-48px)] max-w-4xl'} aria-describedby={undefined} showCloseButton={!pending}>
        <DialogHeader className={action.kind === 'detail' ? 'shrink-0 border-b pb-4' : undefined}><DialogTitle>{titles[action.kind]}</DialogTitle></DialogHeader>
        {loading ? <p role="status">正在加载菜单…</p> : error ? (
          <div className="space-y-3">
            <p role="alert" className="text-sm text-error">{error}</p>
            <Button variant="outline" onClick={() => {
              setError('')
              setLoading(true)
              setAttempt((value) => value + 1)
            }}>重新加载</Button>
          </div>
        ) : menu && (
          action.kind === 'detail' ? (
            <div className="grid min-h-0 flex-1 grid-cols-2 gap-5">
              <section aria-label="基本信息" className="min-w-0 space-y-4 overflow-y-auto overscroll-contain py-1 pr-4">
                <h2 className="text-sm font-semibold">基本信息</h2>
                <dl className="grid grid-cols-[100px_minmax(0,1fr)] gap-x-3 gap-y-4 text-sm">
                  {details.map(([label, value]) => (
                    <div key={label} className="contents">
                      <dt className="text-muted-foreground">{label}</dt>
                      <dd className="whitespace-pre-wrap break-all">{value}</dd>
                    </div>
                  ))}
                </dl>
              </section>
              <section aria-label="路由与权限" className="min-w-0 space-y-4 overflow-y-auto overscroll-contain border-l py-1 pl-5">
                <h2 className="text-sm font-semibold">路由与权限</h2>
                <dl className="grid grid-cols-[90px_minmax(0,1fr)] gap-x-3 gap-y-4 text-sm">
                  {[
                    ['路由 / 外链', menu.routePath],
                    ['组件路径', menu.componentPath],
                    ['绑定权限 ID', menu.permissionId],
                    ['权限编码', menu.permissionCode],
                    ['权限名称', menu.permissionName],
                    ['缓存页面', menu.cache ? '是' : '否'],
                    ['固定页签', menu.affix ? '是' : '否'],
                    ['菜单说明', menu.description],
                  ].map(([label, value]) => <div key={label} className="contents">
                    <dt className="text-muted-foreground">{label}</dt><dd className="whitespace-pre-wrap break-all">{value || '—'}</dd>
                  </div>)}
                </dl>
              </section>
            </div>
          ) : (
            <div className="space-y-4" aria-busy={pending}>
              <div className="rounded-lg bg-secondary p-4 text-sm">
                <p className="break-all font-medium">{menu.name}</p>
                <p className="mt-1 break-all text-muted-foreground">{menu.code}</p>
              </div>
              <p className="text-sm">确定删除此菜单吗？删除后无法恢复，存在任何下级菜单时均不能删除，请先调整或删除下级菜单。</p>
            </div>
          )
        )}
        <div className={action.kind === 'detail'
          ? 'mt-auto flex shrink-0 justify-end gap-3 border-t pt-4'
          : 'flex justify-end gap-3'}>
          <Button type="button" variant="outline" disabled={pending} onClick={onClose}>
            {action.kind === 'delete' ? '取消' : '关闭'}
          </Button>
          {action.kind === 'delete' && (
            <Button type="button" variant="destructive" disabled={loading || pending || Boolean(error) || !menu} onClick={() => void remove()}>
              {pending ? '正在删除…' : '确认删除'}
            </Button>
          )}
        </div>
      </DialogContent>
    </Dialog>
  )
}
