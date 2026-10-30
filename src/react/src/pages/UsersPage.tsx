import { notify } from '@/lib/notify'
import { useEffect, useState, type FormEvent } from 'react'
import { Link } from 'react-router-dom'
import { isAxiosError } from 'axios'
import { ChevronLeft, ChevronRight, Plus, RotateCcw, Search } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Select } from '@/components/ui/select'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { refreshProfile, useSession } from '@/services/auth'
import { listUsers, type IManagedUser, type IUserPage } from '@/services/users'
import { UserFormDialog } from '@/components/UserFormDialog'
import { userError } from '@/lib/api-error'
import { UserActionDialog, type UserAction } from '@/components/UserActionDialog'

export function UsersPage() {
  const session = useSession()
  const canManage = Boolean(session?.user.canManageUsers)
  const [filters, setFilters] = useState({ keyword: '', phone: '', email: '', status: '', page: 1 })
  const [keyword, setKeyword] = useState('')
  const [phone, setPhone] = useState('')
  const [email, setEmail] = useState('')
  const [status, setStatus] = useState('')
  const [revision, setRevision] = useState(0)
  const [data, setData] = useState<IUserPage | null>(null)
  const [loading, setLoading] = useState(true)
  const [forbidden, setForbidden] = useState(false)
  const [error, setError] = useState('')

  const [open, setOpen] = useState(false)
  const [editing, setEditing] = useState<IManagedUser | null>(null)
  const [action, setAction] = useState<UserAction | null>(null)
  useEffect(() => {
    let active = true
    async function load() {
      try {
        if (!canManage) { setForbidden(true); return }
        setForbidden(false)
        const result = await listUsers(filters.keyword, filters.status, filters.page, filters.phone, filters.email)
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
  }, [filters, revision, canManage])
  function reload() { setLoading(true); setError(''); setRevision((v) => v + 1) }
  function search(event: FormEvent) { event.preventDefault(); setLoading(true); setError(''); setFilters({ keyword: keyword.trim(), phone: phone.trim(), email: email.trim(), status, page: 1 }) }
  function resetSearch() {
    setKeyword('')
    setPhone('')
    setEmail('')
    setStatus('')
    setLoading(true)
    setError('')
    setFilters({ keyword: '', phone: '', email: '', status: '', page: 1 })
  }
  function show(user: IManagedUser | null) { setEditing(user); setOpen(true) }
  if (forbidden) return <section className="space-y-4"><h1 className="text-2xl font-semibold">无权访问用户管理</h1><p className="text-muted-foreground">此功能仅对管理员开放。</p><Button asChild><Link to="/">返回账户中心</Link></Button></section>
  const total = data?.total ?? 0
  const pageSize = data?.size ?? 10
  const pageCount = Math.max(1, Math.ceil(total / pageSize))
  const firstRow = total ? (filters.page - 1) * pageSize + 1 : 0
  const lastRow = Math.min(filters.page * pageSize, total)
  function changePage(page: number) {
    setLoading(true); setError(''); setFilters({ ...filters, page })
  }
  return <section className="w-full space-y-4" aria-labelledby="users-title">
    <header className="flex min-h-10 items-center justify-between gap-4">
      <h1 id="users-title" className="text-2xl font-semibold tracking-tight">用户管理</h1>
      <Button type="button" className="ml-auto h-10 shrink-0 rounded-lg" disabled={loading || !session?.user.canManageUsers} onClick={() => show(null)}><Plus />新增用户</Button>
    </header>

    <form onSubmit={search} aria-label="用户筛选" className="flex flex-wrap items-center justify-between gap-3 rounded-lg border bg-card p-4">
        <div className="flex min-w-0 flex-wrap items-center gap-3">
          <div className="relative w-72 xl:w-80">
            <Search aria-hidden="true" className="pointer-events-none absolute left-3 top-3 z-10 size-4 text-muted-foreground" />
            <Input clearLabel="清空账号或姓名" aria-label="搜索用户" placeholder="搜索账号或姓名" maxLength={100} value={keyword} onChange={(e) => setKeyword(e.target.value)} className="h-10 w-full rounded-lg border-input bg-card pl-9 shadow-none" />
          </div>
          <Input clearLabel="清空手机号" containerClassName="w-44" aria-label="搜索手机号" placeholder="手机号" maxLength={16} value={phone} onChange={(e) => setPhone(e.target.value)} className="h-10 w-full rounded-lg border-input bg-card shadow-none" />
          <Input clearLabel="清空邮箱" containerClassName="w-56" aria-label="搜索邮箱" placeholder="邮箱" maxLength={254} value={email} onChange={(e) => setEmail(e.target.value)} className="h-10 w-full rounded-lg border-input bg-card shadow-none" />
          <Select clearValue="all" aria-label="筛选状态" className="w-40" value={status || 'all'} onValueChange={(value) => setStatus(value === 'all' ? '' : value)} options={[{ value: 'all', label: '全部状态' }, { value: 'true', label: '已启用' }, { value: 'false', label: '已停用' }]} />
          <Button type="submit" className="h-10 rounded-lg px-5" disabled={loading}><Search />搜索</Button>
          <Button type="button" variant="outline" className="h-10 rounded-lg px-4" disabled={loading} onClick={resetSearch}><RotateCcw aria-hidden="true" />重置</Button>
        </div>
    </form>

    <div className="overflow-hidden rounded-lg border bg-card">
      <div aria-busy={loading}>
        <Table className="min-w-[1320px] table-fixed [&_th]:px-5 [&_td]:px-5">
          <colgroup><col className="w-[20%]" /><col /><col className="w-40" /><col className="w-56" /><col className="w-28" /><col className="w-48" /><col className="w-64" /></colgroup>
          <TableHeader className="bg-secondary">
            <TableRow className="hover:bg-transparent">
              <TableHead className="h-11 text-xs font-semibold text-foreground">账号</TableHead>
              <TableHead className="h-11 text-xs font-semibold text-foreground">姓名</TableHead>
              <TableHead className="h-11 text-xs font-semibold text-foreground">手机号</TableHead>
              <TableHead className="h-11 text-xs font-semibold text-foreground">邮箱</TableHead>
              <TableHead className="h-11 text-xs font-semibold text-foreground">状态</TableHead>
              <TableHead className="h-11 text-xs font-semibold text-foreground">创建时间</TableHead>
              <TableHead className="h-11 text-right text-xs font-semibold text-foreground">操作</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {loading ? <TableRow><TableCell colSpan={7} className="h-64 text-center text-muted-foreground">正在加载用户…</TableCell></TableRow>
              : error ? <TableRow><TableCell colSpan={7} className="h-64 text-center">加载失败。<Button variant="link" onClick={reload}>重试</Button></TableCell></TableRow>
              : !data?.items.length ? <TableRow><TableCell colSpan={7} className="h-64 text-center text-muted-foreground">暂无符合条件的用户</TableCell></TableRow>
              : data.items.map((user) => <TableRow key={user.id} className="h-14 even:bg-muted/60 hover:bg-primary/[0.045]">
                <TableCell><div className="flex min-w-0 items-center gap-2"><span className="truncate font-medium" title={user.username}>{user.username}</span>{user.id === session?.user.id && <span className="shrink-0 rounded bg-secondary px-1.5 py-0.5 text-[11px] text-muted-foreground">本人</span>}</div></TableCell>
                <TableCell className="truncate" title={user.displayName}>{user.displayName}</TableCell>
                <TableCell className="truncate tabular-nums" title={user.phone ?? undefined}>{user.phone || '—'}</TableCell>
                <TableCell className="truncate" title={user.email ?? undefined}>{user.email || '—'}</TableCell>
                <TableCell><span className={user.enabled ? 'inline-flex items-center gap-1.5 rounded-md bg-primary/10 px-2 py-1 text-xs font-medium text-primary' : 'inline-flex items-center gap-1.5 rounded-md bg-secondary px-2 py-1 text-xs text-muted-foreground'}><span aria-hidden="true" className={user.enabled ? 'size-1.5 rounded-full bg-primary' : 'size-1.5 rounded-full bg-black/30'} />{user.enabled ? '已启用' : '已停用'}</span></TableCell>
                <TableCell className="text-xs tabular-nums text-muted-foreground">{user.createdAt.replace('T', ' ').slice(0, 19)}</TableCell>
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

      <footer className="flex flex-wrap items-center justify-between gap-4 border-t bg-muted/50 p-4 text-sm">
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
    {open && <UserFormDialog user={editing} currentUserId={session?.user.id} onClose={() => setOpen(false)} onSuccess={(message) => {
      setOpen(false)
      notify.success(message)
      reload()
      if (editing?.id === session?.user.id) {
        void refreshProfile().catch((cause: unknown) => notify.error(userError(cause)))
      }
    }} />}
  </section>
}
