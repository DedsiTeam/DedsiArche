import { notify } from '@/lib/notify'
import { useEffect, useState, type FormEvent } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import { isAxiosError } from 'axios'
import { Eye, EyeOff, Layers2, LoaderCircle } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { clearLocalSession, login } from '@/services/auth'
import { validateLogin } from '@/lib/login-validation'

export function LoginPage() {
  const navigate = useNavigate()
  const location = useLocation()
  const from: unknown = location.state?.from
  const destination = typeof from === 'string' && from.startsWith('/') &&
    !from.startsWith('//') && !from.includes('\\') && !/^\/login(?:[/?#]|$)/i.test(from) ? from : '/'
  const [username, setUsername] = useState(typeof location.state?.username === 'string' ? location.state.username : '')
  const [password, setPassword] = useState('')
  const [visible, setVisible] = useState(false)
  const [pending, setPending] = useState(false)

  const [fieldErrors, setFieldErrors] = useState({ username: '', password: '' })

  useEffect(() => {
    // 每次进入登录页先清除内存会话，使旧的用户资料请求失效。
    clearLocalSession()
    // 两种存储分别清空，避免其中一种被浏览器禁用时阻止另一种清理。
    try {
      localStorage.clear()
    } catch { /* 浏览器禁止访问时无法清理该存储，内存会话已清除。 */ }
    try {
      sessionStorage.clear()
    } catch { /* 浏览器禁止访问时无法清理该存储，内存会话已清除。 */ }
  }, [location.key])

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (pending) return

    const errors = validateLogin(username, password)
    setFieldErrors(errors)
    if (errors.username || errors.password) {
      event.currentTarget.querySelector<HTMLInputElement>(errors.username ? '#username' : '#password')?.focus()
      return
    }
    setPending(true)
    try {
      await login(username, password)
      setPassword('')
      navigate(destination, { replace: true })
    } catch (cause) {
      if (isAxiosError(cause)) {
        const status = cause.response?.status
        if (cause.response?.data?.code === 'USER_DISABLED') { notify.error('当前账户已停用，请联系管理员。'); return }
        notify.error(status === 401 ? '账号或密码不正确，请重新输入。' : status === 403 ? '当前账户无法登录，请联系管理员。' : status === 400 ? '请输入有效的账号和密码。' : status === 429 ? '尝试次数过多，请稍后再试。' : '暂时无法连接登录服务，请稍后重试。')
      } else notify.error('登录失败，请稍后重试。')
    } finally { setPending(false) }
  }

  return (
    <main className="login-page">
      <div className="login-shell">
        <aside className="login-brand" aria-label="DedsiArche 后台管理系统">
          <div className="flex items-center gap-3 text-xl font-semibold"><Layers2 aria-hidden="true" className="size-8" />DedsiArche</div>
          <div className="login-brand-art" aria-hidden="true"><Layers2 strokeWidth={0.65} /></div>
          <div><p className="whitespace-nowrap text-3xl font-semibold leading-relaxed">始于本原，成于至简。</p><p className="mt-4 text-sm text-[var(--login-brand-muted)]">后台管理系统</p></div>
        </aside>
        <section aria-labelledby="login-title" className="login-panel">
        <div className="login-compact-brand"><Layers2 aria-hidden="true" className="size-7" /><span>DedsiArche</span></div>
        <h1 id="login-title" className="text-3xl font-semibold tracking-tight">账号登录</h1>

        <form onSubmit={submit} noValidate className="mt-10 space-y-5" aria-busy={pending}>
          <div>
            <label htmlFor="username" className="mb-2 block text-sm font-medium">账号</label>
            <Input id="username" name="username" autoComplete="username" autoCapitalize="none" spellCheck={false} required value={username} onChange={(event) => { setUsername(event.target.value); setFieldErrors((value) => ({ ...value, username: '' })) }} onBlur={() => setFieldErrors((value) => ({ ...value, username: validateLogin(username, password).username }))} disabled={pending} placeholder="请输入账号" className="h-12 rounded-lg bg-card px-4 text-base shadow-none" aria-invalid={!!fieldErrors.username} aria-describedby={fieldErrors.username ? 'username-error' : undefined} />
            {fieldErrors.username && <p id="username-error" role="alert" className="mt-2 text-sm text-error">{fieldErrors.username}</p>}
          </div>
          <div>
            <label htmlFor="password" className="mb-2 block text-sm font-medium">密码</label>
            <div className="relative">
              <Input hasEndAction id="password" name="password" type={visible ? 'text' : 'password'} autoComplete="current-password" required value={password} onChange={(event) => { setPassword(event.target.value); setFieldErrors((value) => ({ ...value, password: '' })) }} onBlur={() => setFieldErrors((value) => ({ ...value, password: validateLogin(username, password).password }))} disabled={pending} placeholder="请输入密码" className="h-12 rounded-lg bg-card pl-4 pr-14 text-base shadow-none" aria-invalid={!!fieldErrors.password} aria-describedby={fieldErrors.password ? 'password-error' : undefined} />
              <Button type="button" variant="ghost" size="icon" disabled={pending} onClick={() => setVisible((value) => !value)} aria-label={visible ? '隐藏密码' : '显示密码'} aria-pressed={visible} className="absolute right-0.5 top-0.5 size-11 rounded-md text-muted-foreground">{visible ? <EyeOff /> : <Eye />}</Button>
            </div>
            {fieldErrors.password && <p id="password-error" role="alert" className="mt-2 text-sm text-error">{fieldErrors.password}</p>}
          </div>

          <Button type="submit" disabled={pending} className="mt-2 h-12 w-full rounded-lg text-base font-semibold text-primary-foreground">{pending && <LoaderCircle aria-hidden="true" className="animate-spin" />}{pending ? '正在登录…' : '登录'}</Button>
        </form>

        </section>
      </div>
    </main>
  )
}
