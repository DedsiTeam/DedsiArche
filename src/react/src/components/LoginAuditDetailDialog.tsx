import { useEffect, useState } from 'react'
import { Button } from '@/components/ui/button'
import { Dialog, DialogContent, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { loginAuditDetail, loginAuditReasonLabels, type ILoginAudit } from '@/services/login-audits'
import { loginAuditError } from '@/lib/login-audit-validation'

interface ILoginAuditDetailDialogProps {
  id: string
  onClose: () => void
}

/** 仅展示审计快照；用户输入与失败说明作为文本渲染，不解释为 HTML。 */
export function LoginAuditDetailDialog({ id, onClose }: ILoginAuditDetailDialogProps) {
  const [audit, setAudit] = useState<ILoginAudit | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [attempt, setAttempt] = useState(0)
  useEffect(() => {
    const controller = new AbortController()
    setAudit(null)
    setLoading(true)
    setError('')
    loginAuditDetail(id, controller.signal)
      .then((result) => {
        if (!controller.signal.aborted) setAudit(result)
      })
      .catch((cause: unknown) => {
        if (!controller.signal.aborted) setError(loginAuditError(cause))
      })
      .finally(() => {
        if (!controller.signal.aborted) setLoading(false)
      })
    return () => controller.abort()
  }, [id, attempt])

  const groups = audit ? [
    {
      title: '登录信息',
      fields: [
        ['审计 ID', audit.id],
        ['登录时间', audit.loginTime.replace('T', ' ').slice(0, 19)],
        ['登录结果', audit.result === 'SUCCESS' ? '成功' : '失败'],
        ['结果原因', loginAuditReasonLabels[audit.reasonCode] ?? audit.reasonCode],
        ['登录账号', audit.loginAccount],
        ['用户 ID', audit.userId],
        ['姓名', audit.displayName],
      ],
    },
    {
      title: '安全信息',
      fields: [
        ['客户端 IP', audit.clientIp],
        ['客户端信息', audit.userAgent],
        ['失败说明', audit.failureMessage],
        ['追踪 ID', audit.traceId],
        ['原因编码', audit.reasonCode],
      ],
    },
  ] : []

  return (
    <Dialog open onOpenChange={(open) => { if (!open) onClose() }}>
      <DialogContent className="flex max-h-[82vh] w-[calc(100vw-48px)] max-w-[1100px] flex-col gap-5 overflow-hidden bg-card" aria-describedby={undefined}>
        <DialogHeader className="shrink-0 border-b pb-4">
          <DialogTitle>登录审计详情</DialogTitle>
        </DialogHeader>
        {loading ? <p role="status" className="text-sm text-muted-foreground">正在加载登录审计…</p>
          : error ? <div className="space-y-3">
            <p role="alert" className="text-sm text-error">{error}</p>
            <Button type="button" variant="outline" onClick={() => {
              setLoading(true)
              setError('')
              setAttempt((value) => value + 1)
            }}>重新加载</Button>
          </div>
            : <div className="grid min-h-0 flex-1 grid-cols-2 gap-5 overflow-hidden">
              {groups.map((group, index) => (
                <section key={group.title} aria-label={group.title} className={`min-w-0 space-y-4 overflow-y-auto overscroll-contain py-1 ${index ? 'border-l pl-5' : 'pr-4'}`}>
                  <h2 className="text-sm font-semibold">{group.title}</h2>
                  <dl className="grid grid-cols-[100px_minmax(0,1fr)] gap-x-3 gap-y-4 text-sm">
                    {group.fields.map(([label, value]) => (
                      <div key={label} className="contents">
                        <dt className="text-muted-foreground">{label}</dt>
                        <dd className="whitespace-pre-wrap break-all">{value || '—'}</dd>
                      </div>
                    ))}
                  </dl>
                </section>
              ))}
            </div>}
        <div className="flex shrink-0 justify-end border-t pt-4">
          <Button type="button" variant="outline" onClick={onClose}>关闭</Button>
        </div>
      </DialogContent>
    </Dialog>
  )
}
