import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { isAxiosError } from 'axios'
import { ChevronLeft, ChevronRight } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { LoginAuditFilters } from '@/components/LoginAuditFilters'
import { LoginAuditDetailDialog } from '@/components/LoginAuditDetailDialog'
import { useSession } from '@/services/auth'
import { listLoginAudits, loginAuditReasonLabels, type ILoginAuditFilters, type ILoginAuditPage } from '@/services/login-audits'
import { loginAuditError } from '@/lib/login-audit-validation'
import { notify } from '@/lib/notify'

/** 登录审计只读查询；筛选草稿与已提交条件独立，避免键入即请求。 */
export function LoginAuditsPage() {
  const session = useSession()
  const canManage = Boolean(session?.user.canManageUsers)
  const [filters, setFilters] = useState<ILoginAuditFilters>({
    loginAccount: '', displayName: '', clientIp: '', result: '', reasonCode: '', from: '', to: '', page: 1,
  })
  const [revision, setRevision] = useState(0)
  const [data, setData] = useState<ILoginAuditPage | null>(null)
  const [loading, setLoading] = useState(true)
  const [forbidden, setForbidden] = useState(false)
  const [error, setError] = useState('')
  const [detailId, setDetailId] = useState<string | null>(null)

  useEffect(() => {
    const controller = new AbortController()
    async function load() {
      setLoading(true)
      setError('')
      try {
        // 登录审计接口与用户管理使用同一个后端管理员策略。
        if (!canManage) {
          setForbidden(true)
          setLoading(false)
          return
        }
        setForbidden(false)
        const result = await listLoginAudits(filters, controller.signal)
        if (controller.signal.aborted) return
        const lastPage = Math.max(1, Math.ceil(result.total / result.size))
        if (filters.page > lastPage) {
          // 回退期间继续 loading，下一轮请求成功后才恢复操作。
          setFilters({ ...filters, page: lastPage })
          return
        }
        setData(result)
        setLoading(false)
      } catch (cause) {
        if (controller.signal.aborted) return
        const message = loginAuditError(cause)
        setError(message)
        setLoading(false)
        notify.error(message)
        if (isAxiosError(cause) && cause.response?.status === 403) setForbidden(true)
      }
    }
    void load()
    return () => controller.abort()
  }, [filters, revision, canManage])

  function reload() {
    setLoading(true)
    setError('')
    setRevision((value) => value + 1)
  }

  function changePage(page: number) {
    setLoading(true)
    setError('')
    setFilters({ ...filters, page })
  }

  if (forbidden) {
    return (
      <section className="space-y-4">
        <h1 className="text-2xl font-semibold">无权访问登录审计</h1>
        <p className="text-muted-foreground">此功能仅对管理员开放。</p>
        <Button asChild><Link to="/">返回账户中心</Link></Button>
      </section>
    )
  }

  const total = data?.total ?? 0
  const pageSize = data?.size ?? 10
  const pageCount = Math.max(1, Math.ceil(total / pageSize))
  const firstRow = total ? (filters.page - 1) * pageSize + 1 : 0
  const lastRow = Math.min(filters.page * pageSize, total)

  return (
    <section className="w-full space-y-4" aria-labelledby="login-audits-title">
      <header className="flex min-h-10 items-center justify-between gap-4">
        <h1 id="login-audits-title" className="text-2xl font-semibold tracking-tight">登录审计</h1>
      </header>

      <LoginAuditFilters loading={loading} onSearch={(values) => {
        setLoading(true)
        setError('')
        setFilters({ ...values, page: 1 })
      }} />

      <div className="min-w-0 overflow-hidden rounded-lg border bg-card">
        <div aria-busy={loading}>
          <Table className="w-full table-auto [&_th]:whitespace-nowrap [&_th]:px-3 [&_td]:whitespace-nowrap [&_td]:px-3">
            <TableHeader className="bg-secondary">
              <TableRow className="hover:bg-transparent">
                {['ID', '登录时间', '结果', '结果原因', '登录账号', '姓名', '客户端 IP', '失败说明'].map((label) => (
                  <TableHead key={label} className="h-11 text-xs font-semibold text-foreground">{label}</TableHead>
                ))}
                <TableHead className="table-action-column h-11 w-px text-right text-xs font-semibold text-foreground">操作</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {loading ? (
                <TableRow><TableCell colSpan={9} className="h-64 text-center text-muted-foreground">正在加载登录审计…</TableCell></TableRow>
              ) : error ? (
                <TableRow>
                  <TableCell colSpan={9} className="h-64 text-center">
                    加载失败。<Button variant="link" onClick={reload}>重试</Button>
                  </TableCell>
                </TableRow>
              ) : !data?.items.length ? (
                <TableRow><TableCell colSpan={9} className="h-64 text-center text-muted-foreground">暂无符合条件的登录审计</TableCell></TableRow>
              ) : data.items.map((audit) => (
                <TableRow key={audit.id} className="h-14 even:bg-muted/60 hover:bg-primary/[0.045]">
                  <TableCell className="font-mono text-xs leading-5 text-muted-foreground">{audit.id}</TableCell>
                  <TableCell className="text-xs tabular-nums">{audit.loginTime.replace('T', ' ').slice(0, 19)}</TableCell>
                  <TableCell>
                    <span className={audit.result === 'SUCCESS'
                      ? 'inline-flex rounded-md bg-primary/10 px-2 py-1 text-xs font-medium text-primary'
                      : 'inline-flex rounded-md bg-error/10 px-2 py-1 text-xs font-medium text-error'}>
                      {audit.result === 'SUCCESS' ? '成功' : '失败'}
                    </span>
                  </TableCell>
                  <TableCell>{loginAuditReasonLabels[audit.reasonCode] ?? audit.reasonCode}</TableCell>
                  <TableCell className="max-w-60 truncate" title={audit.loginAccount ?? undefined}>{audit.loginAccount || '—'}</TableCell>
                  <TableCell className="max-w-40 truncate" title={audit.displayName ?? undefined}>{audit.displayName || '—'}</TableCell>
                  <TableCell className="font-mono text-xs">{audit.clientIp || '—'}</TableCell>
                  <TableCell className="max-w-72 truncate text-muted-foreground" title={audit.failureMessage ?? undefined}>{audit.failureMessage || '—'}</TableCell>
                  <TableCell className="table-action-column w-px">
                    <Button size="sm" variant="ghost" className="px-2 text-primary" aria-label={`查看 ${audit.loginAccount || '未知账号'} 的登录审计详情`} onClick={() => setDetailId(audit.id)}>
                      详情
                    </Button>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </div>
        <footer className="flex flex-wrap items-center justify-between gap-4 border-t bg-muted/50 p-4 text-sm">
          <p className="text-muted-foreground">
            共 <span className="font-medium tabular-nums text-foreground">{total}</span> 条登录审计
            {!loading && !error && total > 0 && <span className="ml-3 text-xs">当前显示 {firstRow}–{lastRow} 条</span>}
          </p>
          <nav aria-label="登录审计列表分页" className="flex items-center gap-2">
            <span className="mr-3 text-xs text-muted-foreground">每页 {pageSize} 条</span>
            <Button variant="outline" size="sm" className="h-9 rounded-lg" disabled={loading || Boolean(error) || filters.page <= 1} onClick={() => changePage(filters.page - 1)}>
              <ChevronLeft aria-hidden="true" />上一页
            </Button>
            <span aria-current="page" aria-label={`当前第 ${filters.page} 页`} className="flex size-9 items-center justify-center rounded-lg bg-primary/10 font-medium tabular-nums text-primary">{filters.page}</span>
            <span className="px-1 text-xs text-muted-foreground">/ {pageCount} 页</span>
            <Button variant="outline" size="sm" className="h-9 rounded-lg" disabled={loading || Boolean(error) || !data || filters.page >= pageCount || filters.page >= 1000000} onClick={() => changePage(filters.page + 1)}>
              下一页<ChevronRight aria-hidden="true" />
            </Button>
          </nav>
        </footer>
      </div>
      {detailId && <LoginAuditDetailDialog key={detailId} id={detailId} onClose={() => setDetailId(null)} />}
    </section>
  )
}
