import { useEffect, useState, type FormEvent } from 'react'
import { Link } from 'react-router-dom'
import { isAxiosError } from 'axios'
import { ChevronLeft, ChevronRight, Plus, RotateCcw, Search } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Select } from '@/components/ui/select'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { DictionaryFormDialog } from '@/components/DictionaryFormDialog'
import { PermissionSystemSelect } from '@/components/PermissionSystemSelect'
import { DictionaryActionDialog, type DictionaryAction } from '@/components/DictionaryActionDialog'
import { useSession } from '@/services/auth'
import {
  listDictionaries, dictionaryEnabledOptions,
  type IDictionaryFilters, type IDictionaryPage, type DictionaryEnabled,
} from '@/services/dictionaries'
import { isDictionaryEnabled, dictionaryError } from '@/lib/dictionary-validation'
import { notify } from '@/lib/notify'

/** 字典目录管理；筛选草稿与已提交条件独立，避免键入即请求。 */
export function DictionariesPage() {
  const session = useSession()
  const canManage = Boolean(session?.user.canManageUsers)
  const [keyword, setKeyword] = useState('')
  const [status, setStatus] = useState<DictionaryEnabled>('')
  const [systemCode, setSystemCode] = useState('')
  const [filters, setFilters] = useState<IDictionaryFilters>({ keyword: '', systemCode: '', status: '', page: 1 })
  const [revision, setRevision] = useState(0)
  const [data, setData] = useState<IDictionaryPage | null>(null)
  const [loading, setLoading] = useState(true)
  const [forbidden, setForbidden] = useState(false)
  const [error, setError] = useState('')
  const [creating, setCreating] = useState(false)
  const [action, setAction] = useState<DictionaryAction | null>(null)

  useEffect(() => {
    const controller = new AbortController()
    async function load() {
      setLoading(true)
      setError('')
      try {
        // 字典接口与用户管理使用同一个后端管理员策略。
        if (!canManage) {
          setForbidden(true)
          setLoading(false)
          return
        }
        setForbidden(false)
        const result = await listDictionaries(filters, controller.signal)
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
        const message = dictionaryError(cause)
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

  function search(event: FormEvent) {
    event.preventDefault()
    setLoading(true)
    setError('')
    setFilters({ keyword: keyword.trim(), systemCode: systemCode.trim(), status, page: 1 })
  }

  function resetSearch() {
    setKeyword('')
    setSystemCode('')
    setStatus('')
    setLoading(true)
    setError('')
    setFilters({ keyword: '', systemCode: '', status: '', page: 1 })
  }

  function changePage(page: number) {
    setLoading(true)
    setError('')
    setFilters({ ...filters, page })
  }

  function saved(message: string) {
    setCreating(false)
    setAction(null)
    notify.success(message)
    reload()
  }

  if (forbidden) {
    return (
      <section className="space-y-4">
        <h1 className="text-2xl font-semibold">无权访问字典管理</h1>
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
    <section className="w-full space-y-4" aria-labelledby="dictionaries-title">
      <header className="flex min-h-10 items-center justify-between gap-4">
        <h1 id="dictionaries-title" className="text-2xl font-semibold tracking-tight">字典管理</h1>
        <Button type="button" className="ml-auto h-10 shrink-0 rounded-lg" disabled={loading || Boolean(error)} onClick={() => setCreating(true)}>
          <Plus aria-hidden="true" />新增字典
        </Button>
      </header>

      <form onSubmit={search} aria-label="字典筛选" className="flex flex-wrap items-center gap-3 rounded-lg border bg-card p-4">
        <div className="relative w-72 xl:w-80">
          <Search aria-hidden="true" className="pointer-events-none absolute left-3 top-3 z-10 size-4 text-muted-foreground" />
          <Input
            aria-label="搜索字典编码、名称或系统名称"
            clearLabel="清空字典编码或名称"
            placeholder="搜索字典编码、名称或系统名称"
            maxLength={100}
            value={keyword}
            onChange={(event) => setKeyword(event.target.value)}
            className="h-10 rounded-lg bg-card pl-9 shadow-none"
          />
        </div>
        <PermissionSystemSelect
          filter
          className="w-64"
          value={systemCode}
          onChange={setSystemCode}
        />
        <Select
          aria-label="筛选字典状态"
          clearLabel="清空字典状态"
          clearValue="all"
          className="w-40"
          value={status || 'all'}
          options={[{ value: 'all', label: '全部状态' }, ...dictionaryEnabledOptions]}
          onValueChange={(value) => setStatus(isDictionaryEnabled(value) ? value : '')}
        />
        <Button type="submit" className="h-10 rounded-lg px-5" disabled={loading}>
          <Search aria-hidden="true" />搜索
        </Button>
        <Button type="button" variant="outline" className="h-10 rounded-lg px-4" disabled={loading} onClick={resetSearch}>
          <RotateCcw aria-hidden="true" />重置
        </Button>
      </form>

      <div className="overflow-hidden rounded-lg border bg-card">
        <div aria-busy={loading}>
          <Table className="w-full table-auto [&_th]:whitespace-nowrap [&_th]:px-3 [&_td]:whitespace-nowrap [&_td]:px-3">
            <TableHeader className="bg-secondary">
              <TableRow className="hover:bg-transparent">
                {['ID', '字典编码', '字典名称', '所属系统编码', '所属系统名称', '状态', '创建时间'].map((label) => (
                  <TableHead key={label} className="h-11 text-xs font-semibold text-foreground">{label}</TableHead>
                ))}
                <TableHead className="table-action-column h-11 w-px text-right text-xs font-semibold text-foreground">操作</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {loading ? (
                <TableRow><TableCell colSpan={8} className="h-64 text-center text-muted-foreground">正在加载字典…</TableCell></TableRow>
              ) : error ? (
                <TableRow>
                  <TableCell colSpan={8} className="h-64 text-center">
                    加载失败。<Button variant="link" onClick={reload}>重试</Button>
                  </TableCell>
                </TableRow>
              ) : !data?.items.length ? (
                <TableRow><TableCell colSpan={8} className="h-64 text-center text-muted-foreground">暂无符合条件的字典</TableCell></TableRow>
              ) : data.items.map((dictionary) => (
                <TableRow key={dictionary.id} className="h-14 even:bg-muted/60 hover:bg-primary/[0.045]">
                  <TableCell className="font-mono text-xs leading-5 text-muted-foreground">{dictionary.id}</TableCell>
                  <TableCell className="max-w-60 truncate font-medium" title={dictionary.code}>{dictionary.code}</TableCell>
                  <TableCell className="max-w-60 truncate" title={dictionary.name}>{dictionary.name}</TableCell>
                  <TableCell className="font-mono text-muted-foreground">{dictionary.systemCode}</TableCell>
                  <TableCell className="max-w-60 truncate text-muted-foreground" title={dictionary.systemName}>{dictionary.systemName}</TableCell>
                  <TableCell>
                    <span className={dictionary.enabled
                      ? 'inline-flex rounded-md bg-primary/10 px-2 py-1 text-xs font-medium text-primary'
                      : 'inline-flex rounded-md bg-secondary px-2 py-1 text-xs text-muted-foreground'}>
                      {dictionary.enabled ? '已启用' : '已停用'}
                    </span>
                  </TableCell>
                  <TableCell className="text-xs tabular-nums text-muted-foreground">
                    {dictionary.createdAt.replace('T', ' ').slice(0, 19)}
                  </TableCell>
                  <TableCell className="table-action-column w-px">
                    <div className="ml-auto flex w-max flex-nowrap items-center justify-end gap-0.5">
                      <Button size="sm" variant="ghost" className="px-2 text-primary" aria-label={`详情 ${dictionary.name}`} onClick={() => setAction({ kind: 'detail', id: dictionary.id })}>详情</Button>
                      <Button size="sm" variant="ghost" className="px-2 text-primary" aria-label={`编辑 ${dictionary.name}`} onClick={() => setAction({ kind: 'edit', id: dictionary.id })}>编辑</Button>
                      <Button size="sm" variant="ghost" className="px-2 text-muted-foreground" aria-label={`删除 ${dictionary.name}`} onClick={() => setAction({ kind: 'delete', id: dictionary.id })}>删除</Button>
                    </div>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </div>
        <footer className="flex flex-wrap items-center justify-between gap-4 border-t bg-muted/50 p-4 text-sm">
          <p className="text-muted-foreground">
            共 <span className="font-medium tabular-nums text-foreground">{total}</span> 个字典
            {!loading && !error && total > 0 && <span className="ml-3 text-xs">当前显示 {firstRow}–{lastRow} 条</span>}
          </p>
          <nav aria-label="字典列表分页" className="flex items-center gap-2">
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
      {creating && <DictionaryFormDialog dictionary={null} onClose={() => setCreating(false)} onSuccess={saved} />}
      {action && (
        <DictionaryActionDialog
          key={`${action.kind}:${action.id}`}
          action={action}
          onClose={() => setAction(null)}
          onSuccess={saved}
        />
      )}
    </section>
  )
}
