import { useEffect, useRef, useState } from 'react'
import { Button } from '@/components/ui/button'
import { Dialog, DialogContent, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { ManagedSystemFormDialog } from '@/components/ManagedSystemFormDialog'
import {
  deleteManagedSystem, managedSystemDetail, systemStatusLabel,
  type IManagedSystem,
} from '@/services/managed-systems'
import { managedSystemError } from '@/lib/managed-system-validation'
import { notify } from '@/lib/notify'

export type ManagedSystemAction = { kind: 'detail' | 'edit' | 'delete'; id: string }

interface IManagedSystemActionDialogProps {
  action: ManagedSystemAction
  onClose: () => void
  onSuccess: (message: string) => void
}

/** 详情、编辑及删除均先读取最新记录；写入仍使用版本校验。 */
export function ManagedSystemActionDialog({ action, onClose, onSuccess }: IManagedSystemActionDialogProps) {
  const [system, setSystem] = useState<IManagedSystem | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [attempt, setAttempt] = useState(0)
  const [pending, setPending] = useState(false)
  const lock = useRef(false)
  const titles = { detail: '系统详情', edit: '编辑系统', delete: '确认删除系统' }

  useEffect(() => {
    const controller = new AbortController()
    managedSystemDetail(action.id, controller.signal)
      .then((result) => { if (!controller.signal.aborted) setSystem(result) })
      .catch((cause: unknown) => {
        if (!controller.signal.aborted) setError(managedSystemError(cause))
      })
      .finally(() => { if (!controller.signal.aborted) setLoading(false) })
    return () => controller.abort()
  }, [action.id, attempt])

  async function remove() {
    if (!system || lock.current) return
    lock.current = true
    setPending(true)
    try {
      await deleteManagedSystem(system.id, system.version)
      onSuccess(`系统 ${system.name} 已删除。`)
    } catch (cause) {
      notify.error(managedSystemError(cause, 'delete'))
    } finally {
      lock.current = false
      setPending(false)
    }
  }

  if (system && action.kind === 'edit') {
    return <ManagedSystemFormDialog system={system} onClose={onClose} onSuccess={onSuccess} />
  }

  const details = system ? [
    ['系统 ID', system.id],
    ['系统编码', system.code],
    ['系统名称', system.name],
    ['系统状态', systemStatusLabel(system.status)],
    ['入口地址', system.entryUrl || '—'],
    ['排序', String(system.sortOrder)],
    ['系统说明', system.description || '—'],
    ['创建时间', system.createdAt.replace('T', ' ').slice(0, 19)],
    ['修改时间', system.updatedAt.replace('T', ' ').slice(0, 19)],
  ] : []

  return (
    <Dialog open onOpenChange={(open) => { if (!open && !pending) onClose() }}>
      <DialogContent aria-describedby={undefined} showCloseButton={!pending}>
        <DialogHeader><DialogTitle>{titles[action.kind]}</DialogTitle></DialogHeader>
        {loading ? <p role="status">正在加载系统…</p> : error ? (
          <div className="space-y-3">
            <p role="alert" className="text-sm text-error">{error}</p>
            <Button variant="outline" onClick={() => {
              setError('')
              setLoading(true)
              setAttempt((value) => value + 1)
            }}>重新加载</Button>
          </div>
        ) : system && (
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
                <p className="break-all font-medium">{system.name}</p>
                <p className="mt-1 break-all text-muted-foreground">{system.code}</p>
              </div>
              <p className="text-sm">确定删除此系统吗？删除后无法恢复。系统被权限、岗位、字典、组织或菜单引用时不能删除，即使岗位未配置权限也需先处理岗位引用。</p>
            </div>
          )
        )}
        <div className="flex justify-end gap-3">
          <Button type="button" variant="outline" disabled={pending} onClick={onClose}>
            {action.kind === 'delete' ? '取消' : '关闭'}
          </Button>
          {action.kind === 'delete' && (
            <Button type="button" variant="destructive" disabled={loading || pending || !system} onClick={() => void remove()}>
              {pending ? '正在删除…' : '确认删除'}
            </Button>
          )}
        </div>
      </DialogContent>
    </Dialog>
  )
}
