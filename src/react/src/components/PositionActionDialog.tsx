import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { useEffect, useRef, useState } from 'react'
import { Button } from '@/components/ui/button'
import { Dialog, DialogContent, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { PositionFormDialog } from '@/components/PositionFormDialog'
import {
  deletePosition, positionDetail,
  type IPosition,
} from '@/services/positions'
import { positionError } from '@/lib/position-validation'
import { notify } from '@/lib/notify'

export type PositionAction = { kind: 'detail' | 'edit' | 'delete'; id: string }

interface IPositionActionDialogProps {
  action: PositionAction
  onClose: () => void
  onSuccess: (message: string) => void
}

/** 详情、编辑及删除均先读取最新记录；写入仍使用版本校验。 */
export function PositionActionDialog({ action, onClose, onSuccess }: IPositionActionDialogProps) {
  const [position, setPosition] = useState<IPosition | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [attempt, setAttempt] = useState(0)
  const [pending, setPending] = useState(false)
  const lock = useRef(false)
  const titles = { detail: '岗位详情', edit: '编辑岗位', delete: '确认删除岗位' }

  useEffect(() => {
    const controller = new AbortController()
    setPosition(null)
    setLoading(true)
    setError('')
    positionDetail(action.id, controller.signal)
      .then((result) => { if (!controller.signal.aborted) setPosition(result) })
      .catch((cause: unknown) => {
        if (!controller.signal.aborted) setError(positionError(cause))
      })
      .finally(() => { if (!controller.signal.aborted) setLoading(false) })
    return () => controller.abort()
  }, [action.id, attempt])

  async function remove() {
    if (!position || lock.current) return
    lock.current = true
    setPending(true)
    try {
      await deletePosition(position.id, position.version)
      onSuccess(`岗位 ${position.name} 已删除。`)
    } catch (cause) {
      notify.error(positionError(cause, 'delete'))
    } finally {
      lock.current = false
      setPending(false)
    }
  }

  if (!loading && !error && position && action.kind === 'edit') {
    return <PositionFormDialog position={position} onClose={onClose} onSuccess={onSuccess} />
  }

  const details = position ? [
    ['岗位 ID', position.id],
    ['岗位编码', position.code],
    ['岗位名称', position.name],
    ['所属系统编码', position.systemCode ?? '未设置'],
    ['所属系统名称', position.systemName ?? '未设置'],
    ['岗位状态', position.enabled ? '已启用' : '已停用'],
    ['排序', String(position.sortOrder)],
    ['岗位说明', position.description || '—'],
    ['创建时间', position.createdAt.replace('T', ' ').slice(0, 19)],
    ['修改时间', position.updatedAt.replace('T', ' ').slice(0, 19)],
  ] : []

  return (
    <Dialog open onOpenChange={(open) => { if (!open && !pending) onClose() }}>
      <DialogContent className={action.kind === 'detail'
        ? 'flex h-[85vh] max-h-[820px] w-[calc(100vw-48px)] max-w-[1320px] flex-col overflow-hidden'
        : 'w-[calc(100vw-48px)] max-w-4xl'} aria-describedby={undefined} showCloseButton={!pending}>
        <DialogHeader><DialogTitle>{titles[action.kind]}</DialogTitle></DialogHeader>
        {loading ? <p role="status">正在加载岗位…</p> : error ? (
          <div className="space-y-3">
            <p role="alert" className="text-sm text-error">{error}</p>
            <Button variant="outline" onClick={() => {
              setError('')
              setLoading(true)
              setAttempt((value) => value + 1)
            }}>重新加载</Button>
          </div>
        ) : position && (
          action.kind === 'detail' ? (
            <div className="grid min-h-0 flex-1 grid-cols-[minmax(0,1fr)_minmax(0,2fr)] gap-6">
              <section aria-label="基本信息" className="min-w-0 space-y-4 overflow-y-auto overscroll-contain py-1 pr-3">
                <h2 className="text-sm font-semibold">基本信息</h2>
                <dl className="grid grid-cols-[100px_minmax(0,1fr)] gap-4 text-sm">
                  {details.map(([label, value]) => (
                    <div key={label} className="contents">
                      <dt className="text-muted-foreground">{label}</dt>
                      <dd className="whitespace-pre-wrap break-all">{value}</dd>
                    </div>
                  ))}
                </dl>
              </section>
              <section aria-label="岗位权限" className="min-w-0 space-y-3 overflow-y-auto overscroll-contain border-l py-1 pl-6 pr-2">
                <h3 className="text-sm font-semibold">岗位权限（{position.permissions.length}）</h3>
                {position.permissions.length === 0 ? <p className="text-sm text-muted-foreground">此岗位尚未配置权限。</p> : (
                  <div className="rounded-md border">
                    <Table className="w-full table-auto [&_th]:whitespace-nowrap [&_td]:whitespace-nowrap">
                      <TableHeader className="bg-secondary"><TableRow>
                        <TableHead>所属系统编码</TableHead><TableHead>所属系统名称</TableHead>
                        <TableHead>权限编码</TableHead><TableHead>权限名称</TableHead>
                      </TableRow></TableHeader>
                      <TableBody>{position.permissions.map((item) => (
                        <TableRow key={item.id}>
                          <TableCell className="font-mono text-xs">{item.systemCode}</TableCell>
                          <TableCell className="max-w-48 truncate" title={item.systemName}>{item.systemName}</TableCell>
                          <TableCell className="font-mono text-xs">{item.permissionCode}</TableCell>
                          <TableCell className="max-w-48 truncate" title={item.permissionName}>{item.permissionName}</TableCell>
                        </TableRow>
                      ))}</TableBody>
                    </Table>
                  </div>
                )}
              </section>
            </div>
          ) : (
            <div className="space-y-4" aria-busy={pending}>
              <div className="rounded-lg bg-secondary p-4 text-sm">
                <p className="break-all font-medium">{position.name}</p>
                <p className="mt-1 break-all text-muted-foreground">{position.code}</p>
              </div>
              <p className="text-sm">确定删除此岗位吗？删除后无法恢复。岗位仍被用户引用时不能删除，需先解除用户岗位关联。删除成功后会同时移除岗位和其全部权限关联，权限定义仍会保留。</p>
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
            <Button type="button" variant="destructive" disabled={loading || pending || Boolean(error) || !position} onClick={() => void remove()}>
              {pending ? '正在删除…' : '确认删除'}
            </Button>
          )}
        </div>
      </DialogContent>
    </Dialog>
  )
}
