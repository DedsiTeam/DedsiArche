import { notify } from '@/lib/notify'
import { useState, type FormEvent } from 'react'
import { Navigate, useLocation, useNavigate } from 'react-router-dom'
import { isAxiosError } from 'axios'
import { Eye, EyeOff, Layers2, LoaderCircle } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { login, useSession } from '@/services/auth'
import { validateLogin } from '@/lib/login-validation'

export function LoginPage() {
  const navigate = useNavigate()
  const session = useSession()
  const location = useLocation()
  const from: unknown = location.state?.from
  const destination = typeof from === 'string' && from.startsWith('/') &&
    !from.startsWith('//') && !from.includes('\\') && !/^\/login(?:[/?#]|$)/i.test(from) ? from : '/'
  const [username, setUsername] = useState(typeof location.state?.username === 'string' ? location.state.username : '')
  const [password, setPassword] = useState('')
  const [visible, setVisible] = useState(false)
  const [pending, setPending] = useState(false)

  const [fieldErrors, setFieldErrors] = useState({ username: '', password: '' })

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
        notify.error(status === 401 ? '用户名或密码不正确，请重新输入。' : status === 403 ? '当前账户无法登录，请联系管理员。' : status === 400 ? '请输入有效的用户名和密码。' : status === 429 ? '尝试次数过多，请稍后再试。' : '暂时无法连接登录服务，请稍后重试。')
      } else notify.error('登录失败，请稍后重试。')
    } finally { setPending(false) }
  }

  if (session) return <Navigate to={destination} replace />

  return (
    <main className="login-page">
      <section aria-labelledby="login-title" className="login-panel">
        <div aria-label="DedsiArche" className="mb-9 inline-flex size-14 items-center justify-center rounded-2xl bg-foreground text-white"><Layers2 className="size-8" /></div>
        <h1 id="login-title" className="text-3xl font-semibold tracking-tight">登录 DedsiArche</h1>
        <p className="mt-3 text-base leading-7 text-muted-foreground">欢迎回来。<br />登录账户，开始今天的工作。</p>

        <form onSubmit={submit} noValidate className="mt-10 space-y-5" aria-busy={pending}>
          <div>
            <label htmlFor="username" className="sr-only">用户名</label>
            <Input id="username" name="username" autoComplete="username" autoCapitalize="none" spellCheck={false} required value={username} onChange={(event) => { setUsername(event.target.value); setFieldErrors((value) => ({ ...value, username: '' })) }} onBlur={() => setFieldErrors((value) => ({ ...value, username: validateLogin(username, password).username }))} disabled={pending} placeholder="请输入用户名" className="h-14 rounded-xl bg-white px-5 text-base" aria-invalid={!!fieldErrors.username} aria-describedby={fieldErrors.username ? 'username-error' : undefined} />
            {fieldErrors.username && <p id="username-error" role="alert" className="mt-2 text-sm">{fieldErrors.username}</p>}
          </div>
          <div>
            <label htmlFor="password" className="sr-only">密码</label>
            <div className="relative">
              <Input id="password" name="password" type={visible ? 'text' : 'password'} autoComplete="current-password" required value={password} onChange={(event) => { setPassword(event.target.value); setFieldErrors((value) => ({ ...value, password: '' })) }} onBlur={() => setFieldErrors((value) => ({ ...value, password: validateLogin(username, password).password }))} disabled={pending} placeholder="请输入密码" className="h-14 rounded-xl bg-white pl-5 pr-14 text-base" aria-invalid={!!fieldErrors.password} aria-describedby={fieldErrors.password ? 'password-error' : undefined} />
              <Button type="button" variant="ghost" size="icon" disabled={pending} onClick={() => setVisible((value) => !value)} aria-label={visible ? '隐藏密码' : '显示密码'} aria-pressed={visible} className="absolute right-3 top-2.5 text-muted-foreground">{visible ? <EyeOff /> : <Eye />}</Button>
            </div>
            {fieldErrors.password && <p id="password-error" role="alert" className="mt-2 text-sm">{fieldErrors.password}</p>}
          </div>

          <Button type="submit" disabled={pending} className="h-14 w-full rounded-xl text-base font-semibold text-black/90">{pending && <LoaderCircle className="animate-spin" />}{pending ? '正在登录…' : '登录'}</Button>
        </form>

        <p className="mt-14 text-xs tracking-widest text-muted-foreground">始于本原，成于至简。</p>
      </section>
    </main>
  )
}
