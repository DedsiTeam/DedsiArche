import { useEffect, useRef, useState } from 'react'
import { Button } from '@/components/ui/button'
import { Dialog, DialogContent, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { OrganizationFormDialog } from '@/components/OrganizationFormDialog'
import {
  deleteOrganization, organizationDetail,
  type IOrganization,
} from '@/services/organizations'
import { organizationError } from '@/lib/organization-validation'
import { notify } from '@/lib/notify'

export type OrganizationAction = { kind: 'detail' | 'edit' | 'delete'; id: string }

interface IOrganizationActionDialogProps {
  action: OrganizationAction
  onClose: () => void
  onSuccess: (message: string) => void
}

/** 详情、编辑及删除均先读取最新记录；写入仍使用版本校验。 */
export function OrganizationActionDialog({ action, onClose, onSuccess }: IOrganizationActionDialogProps) {
  const [organization, setOrganization] = useState<IOrganization | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [attempt, setAttempt] = useState(0)
  const [pending, setPending] = useState(false)
  const lock = useRef(false)
  const titles = { detail: '组织详情', edit: '编辑组织', delete: '确认删除组织' }

  useEffect(() => {
    const controller = new AbortController()
    setOrganization(null)
    setLoading(true)
    setError('')
    organizationDetail(action.id, controller.signal)
      .then((result) => { if (!controller.signal.aborted) setOrganization(result) })
      .catch((cause: unknown) => {
        if (!controller.signal.aborted) setError(organizationError(cause))
      })
      .finally(() => { if (!controller.signal.aborted) setLoading(false) })
    return () => controller.abort()
  }, [action.id, attempt])

  async function remove() {
    if (!organization || lock.current) return
    lock.current = true
    setPending(true)
    try {
      await deleteOrganization(organization.id, organization.version)
      onSuccess(`组织 ${organization.name} 已删除。`)
    } catch (cause) {
      notify.error(organizationError(cause, 'delete'))
    } finally {
      lock.current = false
      setPending(false)
    }
  }

  if (!loading && !error && organization && action.kind === 'edit') {
    return <OrganizationFormDialog organization={organization} onClose={onClose} onSuccess={onSuccess} />
  }

  const details = organization ? [
    ['组织 ID', organization.id],
    ['组织编码', organization.code],
    ['组织名称', organization.name],
    ['所属系统编码', organization.systemCode ?? '未设置'],
    ['所属系统名称', organization.systemName ?? '未设置'],
    ['组织状态', organization.enabled ? '已启用' : '已停用'],
    ['上级组织 ID', organization.parentId || '无上级（根组织）'],
    ['排序', String(organization.sortOrder)],
    ['创建时间', organization.createdAt.replace('T', ' ').slice(0, 19)],
    ['修改时间', organization.updatedAt.replace('T', ' ').slice(0, 19)],
  ] : []

  return (
    <Dialog open onOpenChange={(open) => { if (!open && !pending) onClose() }}>
      <DialogContent className={action.kind === 'detail'
        ? 'flex h-[82vh] max-h-[760px] w-[calc(100vw-48px)] max-w-[1100px] flex-col gap-5 overflow-hidden bg-card'
        : 'w-[calc(100vw-48px)] max-w-4xl'} aria-describedby={undefined} showCloseButton={!pending}>
        <DialogHeader className={action.kind === 'detail' ? 'shrink-0 border-b pb-4' : undefined}><DialogTitle>{titles[action.kind]}</DialogTitle></DialogHeader>
        {loading ? <p role="status">正在加载组织…</p> : error ? (
          <div className="space-y-3">
            <p role="alert" className="text-sm text-error">{error}</p>
            <Button variant="outline" onClick={() => {
              setError('')
              setLoading(true)
              setAttempt((value) => value + 1)
            }}>重新加载</Button>
          </div>
        ) : organization && (
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
              <section aria-label="扩展信息" className="min-w-0 space-y-4 overflow-y-auto overscroll-contain border-l py-1 pl-5">
                <h2 className="text-sm font-semibold">扩展信息</h2>
                <dl className="grid grid-cols-[90px_minmax(0,1fr)] gap-x-3 gap-y-4 text-sm">
                  {[
                    ['别名 1', organization.name1], ['别名 2', organization.name2],
                    ['别名 3', organization.name3], ['别名 4', organization.name4],
                    ['组织说明', organization.description],
                  ].map(([label, value]) => <div key={label} className="contents">
                    <dt className="text-muted-foreground">{label}</dt><dd className="whitespace-pre-wrap break-all">{value || '—'}</dd>
                  </div>)}
                </dl>
              </section>
            </div>
          ) : (
            <div className="space-y-4" aria-busy={pending}>
              <div className="rounded-lg bg-secondary p-4 text-sm">
                <p className="break-all font-medium">{organization.name}</p>
                <p className="mt-1 break-all text-muted-foreground">{organization.code}</p>
              </div>
              <p className="text-sm">确定删除此组织吗？删除后无法恢复，存在任何下级组织时均不能删除，请先调整或删除下级组织。</p>
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
            <Button type="button" variant="destructive" disabled={loading || pending || Boolean(error) || !organization} onClick={() => void remove()}>
              {pending ? '正在删除…' : '确认删除'}
            </Button>
          )}
        </div>
      </DialogContent>
    </Dialog>
  )
}
