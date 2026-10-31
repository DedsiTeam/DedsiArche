import { useEffect, useRef, useState } from 'react'
import { Button } from '@/components/ui/button'
import { Dialog, DialogContent, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { PermissionFormDialog } from '@/components/PermissionFormDialog'
import {
  deletePermission, permissionDetail,
  type IPermission,
} from '@/services/permissions'
import { permissionError } from '@/lib/permission-validation'
import { notify } from '@/lib/notify'

export type PermissionAction = { kind: 'detail' | 'edit' | 'delete'; id: string }

interface IPermissionActionDialogProps {
  action: PermissionAction
  onClose: () => void
  onSuccess: (message: string) => void
}

/** 详情、编辑及删除均先读取最新记录；写入仍使用版本校验。 */
export function PermissionActionDialog({ action, onClose, onSuccess }: IPermissionActionDialogProps) {
  const [permission, setPermission] = useState<IPermission | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [attempt, setAttempt] = useState(0)
  const [pending, setPending] = useState(false)
  const lock = useRef(false)
  const titles = { detail: '权限详情', edit: '编辑权限', delete: '确认删除权限' }

  useEffect(() => {
    const controller = new AbortController()
    permissionDetail(action.id, controller.signal)
      .then((result) => { if (!controller.signal.aborted) setPermission(result) })
      .catch((cause: unknown) => {
        if (!controller.signal.aborted) setError(permissionError(cause))
      })
      .finally(() => { if (!controller.signal.aborted) setLoading(false) })
    return () => controller.abort()
  }, [action.id, attempt])

  async function remove() {
    if (!permission || lock.current) return
    lock.current = true
    setPending(true)
    try {
      await deletePermission(permission.id, permission.version)
      onSuccess(`权限 ${permission.name} 已删除。`)
    } catch (cause) {
      notify.error(permissionError(cause, 'delete'))
    } finally {
      lock.current = false
      setPending(false)
    }
  }

  if (permission && action.kind === 'edit') {
    return <PermissionFormDialog permission={permission} onClose={onClose} onSuccess={onSuccess} />
  }

  const details = permission ? [
    ['权限 ID', permission.id],
    ['权限编码', permission.code],
    ['权限名称', permission.name],
    ['所属系统', permission.systemName],
    ['系统编码', permission.systemCode],
    ['权限状态', permission.enabled ? '已启用' : '已停用'],
    ['排序', String(permission.sortOrder)],
    ['权限说明', permission.description || '—'],
    ['创建时间', permission.createdAt.replace('T', ' ').slice(0, 19)],
    ['修改时间', permission.updatedAt.replace('T', ' ').slice(0, 19)],
  ] : []

  return (
    <Dialog open onOpenChange={(open) => { if (!open && !pending) onClose() }}>
      <DialogContent aria-describedby={undefined} showCloseButton={!pending}>
        <DialogHeader><DialogTitle>{titles[action.kind]}</DialogTitle></DialogHeader>
        {loading ? <p role="status">正在加载权限…</p> : error ? (
          <div className="space-y-3">
            <p role="alert" className="text-sm text-error">{error}</p>
            <Button variant="outline" onClick={() => {
              setError('')
              setLoading(true)
              setAttempt((value) => value + 1)
            }}>重新加载</Button>
          </div>
        ) : permission && (
          action.kind === 'detail' ? (
            <dl className="grid grid-cols-[100px_minmax(0,1fr)] gap-4 text-sm">
              {details.map(([label, value]) => (
                <div key={label} className="contents">
                  <dt className="text-muted-foreground">{label}</dt>
                  <dd className="whitespace-pre-wrap break-all">{value}</dd>
                </div>
              ))}
            </dl>
          ) : (
            <div className="space-y-4" aria-busy={pending}>
              <div className="rounded-lg bg-secondary p-4 text-sm">
                <p className="break-all font-medium">{permission.name}</p>
                <p className="mt-1 break-all text-muted-foreground">{permission.code}</p>
              </div>
              <p className="text-sm">确定删除此权限吗？删除后无法恢复。被菜单或岗位引用时不能删除，请先解除关联。此操作会删除该权限定义。</p>
            </div>
          )
        )}
        <div className="flex justify-end gap-3">
          <Button type="button" variant="outline" disabled={pending} onClick={onClose}>
            {action.kind === 'delete' ? '取消' : '关闭'}
          </Button>
          {action.kind === 'delete' && (
            <Button type="button" variant="destructive" disabled={loading || pending || !permission} onClick={() => void remove()}>
              {pending ? '正在删除…' : '确认删除'}
            </Button>
          )}
        </div>
      </DialogContent>
    </Dialog>
  )
}
