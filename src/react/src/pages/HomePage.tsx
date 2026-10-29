import { notify } from '@/lib/notify'
import { useEffect, useState, type FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import { LoaderCircle, RefreshCw, UserRound } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@/components/ui/card'
import { getProfile, updateProfile, refreshSession, logout, useSession, type UserProfile } from '@/services/auth'
import { validateDisplayName } from '@/lib/user-validation'
import { userError } from '@/lib/api-error'

export function HomePage() {
  const session = useSession()
  const navigate = useNavigate()
  const [profile, setProfile] = useState<UserProfile | null>(null)
  const [name, setName] = useState('')
  const [loading, setLoading] = useState(true)
  const [attempt, setAttempt] = useState(0)
  const [busy, setBusy] = useState('')

  const [fieldError, setFieldError] = useState('')


  useEffect(() => {
    let active = true
    getProfile().then((data) => {
      if (active) { setProfile(data); setName(data.displayName) }
    }).catch((cause: unknown) => { if (active) notify.error(userError(cause)) })
      .finally(() => { if (active) setLoading(false) })
    return () => { active = false }
  }, [attempt])

  async function save(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (busy) return
    const validation = validateDisplayName(name)
    setFieldError(validation)

    if (validation) { event.currentTarget.querySelector<HTMLInputElement>('#displayName')?.focus(); return }
    setBusy('save')
    try {
      const data = await updateProfile(name)
      setProfile(data); setName(data.displayName); notify.success('资料已保存。')
    } catch (cause) { notify.error(userError(cause)) }
    finally { setBusy('') }
  }

  async function sessionAction(action: 'refresh' | 'logout') {
    if (busy) return
    setBusy(action)
    try {
      if (action === 'refresh') { await refreshSession(); notify.success('登录有效期已延长。') }
      else { await logout(); navigate('/login', { replace: true }) }
    } catch (cause) { notify.error(userError(cause)) }
    finally { setBusy('') }
  }

  return <section className="mx-auto max-w-6xl space-y-8" aria-labelledby="account-title">
    <header><div className="mb-4 flex size-12 items-center justify-center rounded-xl bg-primary/10 text-primary"><UserRound /></div><h1 id="account-title" className="text-3xl font-semibold">账户中心</h1><p className="mt-3 text-muted-foreground">管理你的个人资料与登录状态。</p></header>


    {loading ? <p role="status" className="flex items-center gap-2 py-8"><LoaderCircle className="size-4 animate-spin" />正在加载个人资料…</p> : !profile ? <Button onClick={() => {  setLoading(true); setAttempt((value) => value + 1) }}>重新加载</Button> : <div className="grid grid-cols-[minmax(0,1.5fr)_minmax(280px,1fr)] items-start gap-6">
      <Card className="shadow-none"><CardHeader><CardTitle>个人资料</CardTitle><CardDescription>显示名称将同步显示在左下角账户区域。</CardDescription></CardHeader><CardContent>
        <form onSubmit={save} noValidate className="space-y-6" aria-busy={busy === 'save'}>
          <div className="space-y-2"><label htmlFor="accountUsername" className="text-sm font-medium">用户名</label><Input id="accountUsername" value={profile.username} readOnly className="h-11 bg-secondary" /><p className="text-xs text-muted-foreground">用于登录，暂不支持修改。</p></div>
          <div className="space-y-2"><label htmlFor="displayName" className="text-sm font-medium">显示名称</label><Input id="displayName" autoComplete="nickname" value={name} disabled={!!busy} onChange={(event) => { setName(event.target.value); setFieldError('') }} onBlur={() => setFieldError(validateDisplayName(name))} aria-invalid={!!fieldError} aria-describedby={fieldError ? 'name-error' : 'name-hint'} className="h-11" />{fieldError ? <p id="name-error" role="alert" className="text-sm">{fieldError}</p> : <p id="name-hint" className="text-xs text-muted-foreground">最多 100 个字符。</p>}</div>
          <div className="flex gap-3"><Button disabled={!!busy} type="submit">{busy === 'save' ? '正在保存…' : '保存资料'}</Button><Button disabled={!!busy || name === profile.displayName} variant="outline" type="button" onClick={() => { setName(profile.displayName); setFieldError('') }}>撤销修改</Button></div>
        </form>
      </CardContent></Card>
      <Card className="shadow-none"><CardHeader><CardTitle>登录状态</CardTitle><CardDescription>当前账户已登录</CardDescription></CardHeader><CardContent className="space-y-6">
        <div><p className="text-sm text-muted-foreground">本次登录有效至</p><p className="mt-2 text-sm font-medium">{session && new Date(session.expiresAt).toLocaleString('zh-CN', { hour12: false })}</p><p className="mt-2 text-xs leading-6 text-muted-foreground">可在到期前延长登录。到期后需重新登录。</p></div>
        <Button variant="outline" className="w-full" disabled={!!busy} onClick={() => sessionAction('refresh')}><RefreshCw className={busy === 'refresh' ? 'animate-spin' : ''} />{busy === 'refresh' ? '正在延长…' : '延长登录'}</Button>
        <Button variant="ghost" className="w-full" disabled={!!busy} onClick={() => sessionAction('logout')}>{busy === 'logout' ? '正在退出…' : '退出登录'}</Button>
      </CardContent></Card>
    </div>}
  </section>
}
