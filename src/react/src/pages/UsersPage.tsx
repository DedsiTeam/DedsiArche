import { notify } from '@/lib/notify'
import { useEffect, useState, type FormEvent } from 'react'
import { Link } from 'react-router-dom'
import { isAxiosError } from 'axios'
import { ChevronLeft, ChevronRight, Plus, RefreshCw, Search } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Select } from '@/components/ui/select'
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogDescription } from '@/components/ui/dialog'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { getProfile, useSession } from '@/services/auth'
import { createUser, editUser, listUsers, type ManagedUser, type UserPage } from '@/services/users'
import { validateDisplayName, validateRegistration } from '@/lib/user-validation'
import { userError } from '@/lib/api-error'
import { UserActionDialog, type UserAction } from '@/components/UserActionDialog'

const blank = { username: '', displayName: '', password: '', confirmPassword: '' }
export function UsersPage() {
  const session = useSession()
  const [filters, setFilters] = useState({ keyword: '', status: '', page: 1 })
  const [keyword, setKeyword] = useState('')
  const [status, setStatus] = useState('')
  const [revision, setRevision] = useState(0)
  const [data, setData] = useState<UserPage | null>(null)
  const [loading, setLoading] = useState(true)
  const [forbidden, setForbidden] = useState(false)
  const [error, setError] = useState('')

  const [open, setOpen] = useState(false)
  const [editing, setEditing] = useState<ManagedUser | null>(null)
  const [values, setValues] = useState(blank)
  const [enabled, setEnabled] = useState(true)
  const [confirmed, setConfirmed] = useState(false)
  const [errors, setErrors] = useState(blank)

  const [saving, setSaving] = useState(false)
  const [action, setAction] = useState<UserAction | null>(null)
  useEffect(() => {
    let active = true
    async function load() {
      try {
        const profile = await getProfile()
        if (!active) return
        if (!profile.canManageUsers) { setForbidden(true); return }
        const result = await listUsers(filters.keyword, filters.status, filters.page)
        if (active) {
          const lastPage = Math.max(1, Math.ceil(result.total / result.size))
          if (filters.page > lastPage) setFilters({ ...filters, page: lastPage })
          else setData(result)
        }
      } catch (cause) {
        if (active) { setError(userError(cause)); notify.error(userError(cause)); if (isAxiosError(cause) && cause.response?.status === 403) setForbidden(true) }
      } finally { if (active) setLoading(false) }
    }
    void load()
    return () => { active = false }
  }, [filters, revision])
  function reload() { setLoading(true); setError(''); setRevision((v) => v + 1) }
  function search(event: FormEvent) { event.preventDefault(); setLoading(true); setError(''); setFilters({ keyword: keyword.trim(), status, page: 1 }) }
  function show(user: ManagedUser | null) {
    setEditing(user); setValues(user ? { ...blank, username: user.username, displayName: user.displayName } : blank)
    setEnabled(user?.enabled ?? true); setConfirmed(false); setErrors(blank);  setOpen(true)
  }
  async function save(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (saving) return
    const validation = editing ? { ...blank, displayName: validateDisplayName(values.displayName) } : validateRegistration(values)
    setErrors(validation)
    const invalid = Object.entries(validation).find(([, value]) => value)
    if (invalid) { event.currentTarget.querySelector<HTMLInputElement>(`#user-${invalid[0]}`)?.focus(); return }
    if (editing?.enabled && !enabled && !confirmed) { notify.error('请确认停用此用户。'); return }
    setSaving(true)
    try {
      if (editing) await editUser(editing.id, { displayName: values.displayName.trim(), enabled, version: editing.version })
      else await createUser({ username: values.username.trim(), displayName: values.displayName.trim(), password: values.password })
      setOpen(false); setValues(blank); notify.success(editing ? '用户资料已更新。' : '用户已创建，可使用设置的密码登录。')
      await getProfile()
      reload()
    } catch (cause) {
      const status = isAxiosError(cause) ? cause.response?.status : undefined
      notify.error(status === 409 && editing ? '资料已被其他操作修改。请关闭此窗口、刷新列表后重试。' : status === 404 ? '该用户不存在，请刷新列表。' : userError(cause))
    } finally { setSaving(false) }
  }
  if (forbidden) return <section className="space-y-4"><h1 className="text-2xl font-semibold">无权访问用户管理</h1><p className="text-muted-foreground">此功能仅对管理员开放。</p><Button asChild><Link to="/">返回账户中心</Link></Button></section>
  const total = data?.total ?? 0
  const pageSize = data?.size ?? 10
  const pageCount = Math.max(1, Math.ceil(total / pageSize))
  const firstRow = total ? (filters.page - 1) * pageSize + 1 : 0
  const lastRow = Math.min(filters.page * pageSize, total)
  function changePage(page: number) {
    setLoading(true); setError(''); setFilters({ ...filters, page })
  }
  return <section className="w-full space-y-6" aria-labelledby="users-title">
    <header className="flex min-h-10 items-center justify-between gap-4">
      <h1 id="users-title" className="text-3xl font-semibold">用户管理</h1>
    </header>

      <form onSubmit={search} aria-label="用户筛选" className="flex flex-wrap items-center justify-between gap-4 py-4">
        <div className="flex min-w-0 flex-1 flex-wrap items-center gap-3">
        <Input aria-label="搜索用户" placeholder="搜索用户名或显示名称" maxLength={100} value={keyword} onChange={(e) => setKeyword(e.target.value)} className="h-10 w-80 rounded-lg xl:w-96" />
        <Select aria-label="筛选状态" className="w-40" value={status || 'all'} onValueChange={(value) => setStatus(value === 'all' ? '' : value)} options={[{ value: 'all', label: '全部状态' }, { value: 'true', label: '已启用' }, { value: 'false', label: '已停用' }]} />
        <div className="flex shrink-0 items-center gap-2">
          <Button type="submit" className="h-10 rounded-lg px-5" disabled={loading}><Search />搜索</Button>
          <Button type="button" variant="outline" className="h-10 rounded-lg px-5" aria-label="刷新列表" disabled={loading} onClick={reload}><RefreshCw className={loading ? 'animate-spin' : undefined} />刷新</Button>
        </div>
        </div>
        <Button type="button" className="ml-auto h-10 shrink-0 rounded-lg" disabled={loading || !session?.user.canManageUsers} onClick={() => show(null)}><Plus />新增用户</Button>
      </form>

    <div className="overflow-hidden rounded-xl border bg-white">
      <div aria-busy={loading}>
        <Table className="min-w-[920px] table-fixed [&_th]:px-5 [&_td]:px-5">
          <colgroup><col /><col /><col className="w-28" /><col className="w-52" /><col className="w-64" /></colgroup>
          <TableHeader className="bg-black/[0.025]">
            <TableRow className="hover:bg-transparent">
              <TableHead className="h-12 text-muted-foreground">用户名</TableHead>
              <TableHead className="h-12 text-muted-foreground">显示名称</TableHead>
              <TableHead className="h-12 text-muted-foreground">状态</TableHead>
              <TableHead className="h-12 text-muted-foreground">创建时间</TableHead>
              <TableHead className="h-12 text-right text-muted-foreground">操作</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {loading ? <TableRow><TableCell colSpan={5} className="h-64 text-center text-muted-foreground">正在加载用户…</TableCell></TableRow>
              : error ? <TableRow><TableCell colSpan={5} className="h-64 text-center">加载失败。<Button variant="link" onClick={reload}>重试</Button></TableCell></TableRow>
              : !data?.items.length ? <TableRow><TableCell colSpan={5} className="h-64 text-center text-muted-foreground">暂无符合条件的用户</TableCell></TableRow>
              : data.items.map((user) => <TableRow key={user.id} className="h-16 hover:bg-primary/[0.035]">
                <TableCell><div className="flex min-w-0 items-center gap-2"><span className="truncate font-medium" title={user.username}>{user.username}</span>{user.id === session?.user.id && <span className="shrink-0 rounded bg-secondary px-1.5 py-0.5 text-[11px] text-muted-foreground">本人</span>}</div></TableCell>
                <TableCell className="truncate" title={user.displayName}>{user.displayName}</TableCell>
                <TableCell><span className={user.enabled ? 'inline-flex items-center gap-1.5 rounded-md bg-primary/10 px-2 py-1 text-xs' : 'inline-flex items-center gap-1.5 rounded-md bg-secondary px-2 py-1 text-xs text-muted-foreground'}><span aria-hidden="true" className={user.enabled ? 'size-1.5 rounded-full bg-primary' : 'size-1.5 rounded-full bg-black/30'} />{user.enabled ? '已启用' : '已停用'}</span></TableCell>
                <TableCell className="text-xs tabular-nums text-muted-foreground">{new Date(user.createdAt).toLocaleString('zh-CN', { hour12: false })}</TableCell>
                <TableCell><div className="flex items-center justify-end gap-0.5">
                  <Button size="sm" variant="ghost" className="px-2 text-primary" aria-label={`详情 ${user.username}`} onClick={() => setAction({ kind: 'detail', user })}>详情</Button>
                  <Button size="sm" variant="ghost" className="px-2 text-primary" aria-label={`编辑 ${user.username}`} onClick={() => show(user)}>编辑</Button>
                  <Button size="sm" variant="ghost" className="px-2" aria-label={`重置密码 ${user.username}`} onClick={() => setAction({ kind: 'reset', user })}>重置密码</Button>
                  <Button size="sm" variant="ghost" className="px-2 text-muted-foreground" disabled={user.id === session?.user.id} aria-label={`删除 ${user.username}`} onClick={() => setAction({ kind: 'delete', user })}>删除</Button>
                </div></TableCell>
              </TableRow>)}
          </TableBody>
        </Table>
      </div>

      <footer className="flex flex-wrap items-center justify-between gap-4 border-t p-4 text-sm">
        <p className="text-muted-foreground">共 <span className="font-medium tabular-nums text-foreground">{total}</span> 位用户{total > 0 && <span className="ml-3 text-xs">当前显示 {firstRow}–{lastRow} 条</span>}</p>
        <nav aria-label="用户列表分页" className="flex items-center gap-2">
          <span className="mr-3 text-xs text-muted-foreground">每页 {pageSize} 条</span>
          <Button variant="outline" size="sm" className="h-9 rounded-lg" disabled={loading || filters.page <= 1} onClick={() => changePage(filters.page - 1)}><ChevronLeft />上一页</Button>
          <span aria-current="page" aria-label={`当前第 ${filters.page} 页`} className="flex size-9 items-center justify-center rounded-lg bg-primary/10 font-medium tabular-nums text-primary">{filters.page}</span>
          <span className="px-1 text-xs text-muted-foreground">/ {pageCount} 页</span>
          <Button variant="outline" size="sm" className="h-9 rounded-lg" disabled={loading || !data || filters.page >= pageCount} onClick={() => changePage(filters.page + 1)}>下一页<ChevronRight /></Button>
        </nav>
      </footer>
    </div>
    {action && <UserActionDialog action={action} onClose={() => setAction(null)} onSuccess={(text) => { notify.success(text); reload() }} />}
    <Dialog open={open} onOpenChange={(next) => { if (!saving) { setOpen(next); if (!next) setValues(blank) } }}><DialogContent showCloseButton={!saving}><DialogHeader><DialogTitle>{editing ? '编辑用户' : '新增用户'}</DialogTitle><DialogDescription>{editing ? '用户名不能修改，停用后用户将无法访问系统。' : '填写账户资料及初始密码，新增用户默认启用。'}</DialogDescription></DialogHeader><form noValidate onSubmit={save} className="space-y-5" aria-busy={saving}>
      {(['username', 'displayName', ...(editing ? [] : ['password', 'confirmPassword'])] as (keyof typeof blank)[]).map((key) => <div className="space-y-2" key={key}><label htmlFor={`user-${key}`} className="text-sm font-medium">{{ username: '用户名', displayName: '显示名称', password: '初始密码', confirmPassword: '确认密码' }[key]}</label><Input id={`user-${key}`} value={values[key]} readOnly={key === 'username' && !!editing} disabled={saving} type={key.toLowerCase().includes('password') ? 'password' : 'text'} autoComplete={key.toLowerCase().includes('password') ? 'new-password' : 'off'} onChange={(e) => { setValues({ ...values, [key]: e.target.value }); setErrors({ ...errors, [key]: '' }) }} aria-invalid={!!errors[key]} aria-describedby={errors[key] ? `user-${key}-error` : undefined} />{errors[key] && <p id={`user-${key}-error`} role="alert" className="text-sm">{errors[key]}</p>}</div>)}
      {!editing && <p className="text-xs text-muted-foreground">用户名为 3～50 位字母、数字或下划线；密码 12～64 个字符、UTF-8 最多 72 字节。</p>}
      {editing && <div className="space-y-2"><label htmlFor="user-enabled" className="text-sm font-medium">账户状态</label><Select id="user-enabled" value={String(enabled)} disabled={saving || editing.id === session?.user.id} onValueChange={(value) => { setEnabled(value === 'true'); setConfirmed(false) }} className="w-full" options={[{ value: 'true', label: '已启用' }, { value: 'false', label: '已停用' }]} />{editing.id === session?.user.id && <p className="text-xs text-muted-foreground">不能停用当前登录账户。</p>}</div>}
      {editing?.enabled && !enabled && <label className="flex items-start gap-2 rounded-lg bg-secondary p-3 text-sm"><input type="checkbox" checked={confirmed} disabled={saving} onChange={(e) => setConfirmed(e.target.checked)} className="mt-1 accent-primary" />确认停用该用户，并撤销其刷新会话。</label>}

      <div className="flex gap-3"><Button type="submit" disabled={saving}>{saving ? '正在保存…' : editing ? '保存修改' : '创建用户'}</Button><Button type="button" variant="outline" disabled={saving} onClick={() => { setOpen(false); setValues(blank) }}>取消</Button></div>
    </form></DialogContent></Dialog>
  </section>
}
