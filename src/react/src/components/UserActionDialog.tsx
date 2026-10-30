import { notify } from '@/lib/notify'
import { useEffect, useState, type FormEvent } from 'react'
import { isAxiosError } from 'axios'
import { Dialog, DialogContent, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { Input } from '@/components/ui/input'
import { PasswordGeneratorInput } from '@/components/ui/password-generator-input'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { Button } from '@/components/ui/button'
import { userDetail, resetUserPassword, deleteUser, type IManagedUser, type IManagedUserDetail } from '@/services/users'
import { clearLocalSession, useSession } from '@/services/auth'
import { validatePasswordChange } from '@/lib/user-validation'
import { userError } from '@/lib/api-error'

export type UserAction = { kind: 'detail' | 'reset' | 'delete'; user: IManagedUser }
export function UserActionDialog({ action, onClose, onSuccess }: { action: UserAction; onClose: () => void; onSuccess: (message: string) => void }) {
  const session = useSession()
  const [user, setUser] = useState<IManagedUserDetail | null>(null)
  const [loading, setLoading] = useState(true)
  const [attempt, setAttempt] = useState(0)
  const [pending, setPending] = useState(false)

  const [password, setPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [errors, setErrors] = useState({ password: '', confirmPassword: '' })
  const titles = { detail: '用户详情', reset: '重置密码', delete: '确认删除用户' }
  useEffect(() => {
    const request = new AbortController()
    setUser(null)
    setLoading(true)
    userDetail(action.user.id, request.signal)
      .then((value) => { if (!request.signal.aborted) setUser(value) })
      .catch((cause: unknown) => {
        if (!request.signal.aborted) {
          notify.error(isAxiosError(cause) && cause.response?.status === 404
            ? '该用户已不存在，请关闭弹窗并刷新列表。' : userError(cause))
        }
      })
      .finally(() => { if (!request.signal.aborted) setLoading(false) })
    return () => request.abort()
  }, [action.user.id, attempt])
  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!user || pending) return

    if (action.kind === 'reset') {
      const validation = validatePasswordChange(password, confirmPassword)
      setErrors(validation)
      if (validation.password || validation.confirmPassword) { event.currentTarget.querySelector<HTMLInputElement>(validation.password ? '#reset-password' : '#reset-confirm')?.focus(); return }
    }
    setPending(true)
    try {
      if (action.kind === 'delete') await deleteUser(user.id, user.version)
      else await resetUserPassword(user.id, password, user.version)
      setPassword(''); setConfirmPassword('')
      if (action.kind === 'reset' && user.id === session?.user.id) { clearLocalSession(); notify.success('密码已重置，请使用新密码登录。'); return }
      onSuccess(action.kind === 'delete' ? `用户 ${user.username} 已删除。` : `用户 ${user.username} 的密码已重置，旧登录会话已失效。`)
      onClose()
    } catch (cause) {
      const status = isAxiosError(cause) ? cause.response?.status : undefined
      notify.error(status === 409 ? '资料已变化，请关闭弹窗、刷新列表后重试。' : status === 404 ? '该用户已不存在，请关闭弹窗并刷新列表。' : isAxiosError(cause) && cause.response?.data?.code === 'PROTECTED_USER' ? '不能删除当前账户或配置中的管理员。' : userError(cause))
    } finally { setPending(false) }
  }
  return <Dialog open onOpenChange={(open) => { if (!open && !pending) onClose() }}><DialogContent className={action.kind === 'detail' ? 'flex h-[85vh] max-h-[820px] w-[calc(100vw-48px)] max-w-[1320px] flex-col overflow-hidden' : undefined} aria-describedby={undefined} showCloseButton={!pending}>
    <DialogHeader><DialogTitle>{titles[action.kind]}</DialogTitle></DialogHeader>
    {loading ? <p role="status">正在加载用户…</p> : user && <>
      {action.kind === 'detail' ? (
        <div className="grid min-h-0 flex-1 grid-cols-[minmax(0,1fr)_minmax(0,2fr)] gap-6">
          <section aria-label="基本信息" className="min-w-0 space-y-4 overflow-y-auto overscroll-contain py-1 pr-3">
            <h2 className="text-sm font-semibold">基本信息</h2>
            <dl className="grid grid-cols-[80px_minmax(0,1fr)] gap-4 text-sm">
              {[
                ['用户 ID', user.id], ['账号', user.username], ['姓名', user.displayName],
                ['手机号', user.phone || '—'], ['邮箱', user.email || '—'],
                ['账户状态', user.enabled ? '已启用' : '已停用'],
                ['创建时间', user.createdAt.replace('T', ' ').slice(0, 19)],
              ].map(([label, value]) => (
                <div key={label} className="contents">
                  <dt className="text-muted-foreground">{label}</dt>
                  <dd className="break-all">{value}</dd>
                </div>
              ))}
            </dl>
          </section>
          <section className="min-w-0 space-y-3 overflow-y-auto overscroll-contain border-l py-1 pl-6 pr-2" aria-label="用户岗位快照">
            <h3 className="text-sm font-medium">用户岗位（{user.positions.length}）</h3>
            <p className="text-xs text-muted-foreground">以下为保存时的岗位及系统快照，重新保存岗位集合时刷新。</p>
            {user.positions.length === 0 ? <p className="text-sm text-muted-foreground">尚未分配岗位。</p> : (
              <div className="rounded-md border">
                <Table className="w-full table-auto [&_th]:whitespace-nowrap [&_td]:whitespace-nowrap">
                  <TableHeader className="bg-secondary"><TableRow>
                    <TableHead>岗位编码</TableHead><TableHead>岗位名称</TableHead>
                    <TableHead>系统编码</TableHead><TableHead>系统名称</TableHead>
                  </TableRow></TableHeader>
                  <TableBody>{user.positions.map((position) => (
                    <TableRow key={position.id} className="even:bg-muted/60">
                      <TableCell className="font-mono text-xs">{position.positionCode}</TableCell>
                      <TableCell className="max-w-48 truncate" title={position.positionName}>{position.positionName}</TableCell>
                      <TableCell className="font-mono text-xs">{position.systemCode ?? '未设置'}</TableCell>
                      <TableCell className="max-w-48 truncate" title={position.systemName ?? undefined}>{position.systemName ?? '未设置'}</TableCell>
                    </TableRow>
                  ))}</TableBody>
                </Table>
              </div>
            )}
          </section>
        </div>
      ) : <form onSubmit={submit} noValidate className="space-y-5" aria-busy={pending}>
        <div className="rounded-lg bg-secondary p-4 text-sm"><p className="break-all font-medium">{user.username}</p><p className="mt-1 break-all text-muted-foreground">{user.displayName}</p></div>
        {action.kind === 'reset' && <>
          <div className="space-y-2"><label htmlFor="reset-password" className="text-sm">新密码</label><PasswordGeneratorInput id="reset-password" value={password} disabled={pending} onValueChange={(value) => { setPassword(value); setErrors((previous) => ({ ...previous, password: '' })) }} onGenerate={() => { setConfirmPassword(''); setErrors({ password: '', confirmPassword: '' }) }} aria-invalid={!!errors.password} aria-describedby={errors.password ? 'reset-password-error' : 'reset-hint'} />{errors.password && <p id="reset-password-error" role="alert" className="text-sm">{errors.password}</p>}</div>
          <div className="space-y-2"><label htmlFor="reset-confirm" className="text-sm">确认新密码</label><Input id="reset-confirm" type="password" autoComplete="new-password" value={confirmPassword} disabled={pending} onChange={(event) => { setConfirmPassword(event.target.value); setErrors({ ...errors, confirmPassword: '' }) }} aria-invalid={!!errors.confirmPassword} aria-describedby={errors.confirmPassword ? 'reset-confirm-error' : undefined} />{errors.confirmPassword && <p id="reset-confirm-error" role="alert" className="text-sm">{errors.confirmPassword}</p>}</div>
          <p id="reset-hint" className="text-xs text-muted-foreground">密码 12～64 个字符，UTF-8 最多 72 字节。{user.id === session?.user.id && '你正在重置自己的密码，成功后将返回登录页。'}</p>
        </>}
        {action.kind === 'delete' && <p className="text-sm">确定删除此用户吗？删除后无法恢复，该用户、其刷新会话及全部用户岗位关联将被移除，岗位和权限定义仍会保留。</p>}
        <div className="flex justify-end gap-3"><Button id={action.kind === 'delete' ? 'cancel-delete' : undefined} type="button" variant="outline" disabled={pending} onClick={onClose}>取消</Button><Button type="submit" disabled={pending || (action.kind === 'delete' && user.id === session?.user.id)} className={action.kind === 'delete' ? 'bg-black/90 text-white hover:bg-black/80' : ''}>{pending ? '正在处理…' : action.kind === 'delete' ? '确认删除' : '确认重置密码'}</Button></div>
      </form>}
    </>}

    {!loading && !user && <Button variant="outline" onClick={() => { setLoading(true);  setAttempt((v) => v + 1) }}>重新加载</Button>}
    {action.kind === 'detail' && <div className="mt-auto flex shrink-0 justify-end border-t pt-4"><Button variant="outline" onClick={onClose}>关闭</Button></div>}
  </DialogContent></Dialog>
}
