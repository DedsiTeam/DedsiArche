import { useEffect, useRef, useState, type Ref } from 'react'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Checkbox } from '@/components/ui/checkbox'
import { ArrowLeft, ArrowRight } from 'lucide-react'
import type { FormControlProps } from '@/components/ui/form'
import { PermissionSystemSelect } from '@/components/PermissionSystemSelect'
import { listPermissions, type IPermissionFilters, type IPermissionPage } from '@/services/permissions'
import type { IPositionPermission } from '@/services/positions'
import { permissionError } from '@/lib/permission-validation'

interface IPermissionLabel {
  id: string
  systemCode: string
  systemName: string
  code: string
  name: string
  enabled?: boolean
}

interface IPositionPermissionSelectProps extends FormControlProps {
  value: string[]
  onChange: (ids: string[]) => void
  onBlur: () => void
  inputRef: Ref<HTMLInputElement>
  initialPermissions: IPositionPermission[]
}

/** 已选 ID 独立于当前搜索页；更换搜索或翻页绝不清空其他页的选择。 */
export function PositionPermissionSelect({ value, onChange, onBlur, inputRef, initialPermissions, ...control }: IPositionPermissionSelectProps) {
  const [keyword, setKeyword] = useState('')
  const [systemCode, setSystemCode] = useState('')
  const [filters, setFilters] = useState<IPermissionFilters>({ keyword: '', systemCode: '', status: '', page: 1 })
  const [data, setData] = useState<IPermissionPage | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [attempt, setAttempt] = useState(0)
  // 勾选只标记待移动项；只有穿梭操作才修改整单权限集合。
  const [checkedLeft, setCheckedLeft] = useState<string[]>([])
  const [checkedRight, setCheckedRight] = useState<string[]>([])
  const labels = useRef(new Map<string, IPermissionLabel>(initialPermissions.map((item) => [item.permissionId, {
    id: item.permissionId, systemCode: item.systemCode, systemName: item.systemName,
    code: item.permissionCode, name: item.permissionName,
  }])))

  useEffect(() => {
    const request = new AbortController()
    setLoading(true)
    setError('')
    listPermissions(filters, request.signal)
      .then((result) => {
        if (request.signal.aborted) return
        const lastPage = Math.max(1, Math.ceil(result.total / result.size))
        if (filters.page > lastPage) {
          setFilters({ ...filters, page: lastPage })
          return
        }
        result.items.forEach((item) => labels.current.set(item.id, item))
        setData(result)
        setLoading(false)
      })
      .catch((cause: unknown) => {
        if (request.signal.aborted) return
        setError(permissionError(cause))
        setLoading(false)
      })
    return () => request.abort()
  }, [filters, attempt])

  function search() {
    setLoading(true)
    setError('')
    setFilters({ keyword: keyword.trim(), systemCode, status: '', page: 1 })
  }

  function check(ids: string[], id: string, selected: boolean) {
    return selected ? [...new Set([...ids, id])] : ids.filter((item) => item !== id)
  }

  const available = data?.items.filter((item) => !value.includes(item.id)) ?? []
  const pendingAdd = checkedLeft.filter((id) => !value.includes(id))
  const pendingRemove = checkedRight.filter((id) => value.includes(id))
  const pageCount = Math.max(1, Math.ceil((data?.total ?? 0) / (data?.size ?? 10)))

  function permissionLabel(id: string) {
    const item = labels.current.get(id)
    return (
      <span className="min-w-0 flex-1 space-y-1 text-sm">
        <span className="block break-words font-medium">{item?.name ?? id}</span>
        {item && <>
          <span className="block break-all font-mono text-xs text-muted-foreground">{item.code}</span>
          <span className="block break-words text-xs text-muted-foreground">{item.systemName}</span>
          <span className="block break-all font-mono text-xs text-muted-foreground">{item.systemCode}</span>
          {item.enabled === false && <span className="block text-xs text-muted-foreground">已停用</span>}
        </>}
      </span>
    )
  }

  return (
    <div className="min-h-0 flex-1">
      <div className="grid h-full min-h-0 grid-cols-[minmax(0,1fr)_auto_minmax(0,1fr)] items-stretch gap-3">
        <section aria-label="可选权限" aria-busy={loading} className="flex min-h-0 min-w-0 flex-col overflow-hidden rounded-lg border bg-card">
          <div className="shrink-0 space-y-3 border-b bg-muted/30 p-3">
            <h3 className="text-sm font-semibold">可选权限</h3>
            <Input
              {...control}
              ref={inputRef}
              value={keyword}
              onChange={(event) => setKeyword(event.target.value)}
              onBlur={onBlur}
              onKeyDown={(event) => {
                if (event.key === 'Enter' && !event.nativeEvent.isComposing) {
                  event.preventDefault()
                  if (!control.disabled) search()
                }
              }}
              placeholder="搜索权限编码、名称或系统"
              aria-label="搜索可选权限"
              maxLength={100}
            />
            <PermissionSystemSelect filter value={systemCode} onChange={setSystemCode} disabled={control.disabled} className="w-full" />
            <div className="flex flex-wrap gap-2">
              <Button type="button" size="sm" disabled={control.disabled || loading} onClick={search}>搜索</Button>
              <Button type="button" size="sm" variant="outline" disabled={control.disabled || loading} onClick={() => {
                setKeyword('')
                setSystemCode('')
                setLoading(true)
                setError('')
                setFilters({ keyword: '', systemCode: '', status: '', page: 1 })
              }}>重置</Button>
              <Button type="button" size="sm" variant="ghost" disabled={control.disabled || loading || Boolean(error) || !available.length} onClick={() => {
                setCheckedLeft([...new Set([...checkedLeft, ...available.map((item) => item.id)])])
              }}>勾选本页</Button>
            </div>
          </div>
          <div className="min-h-0 flex-1 overflow-y-auto overscroll-contain">
            {loading ? <p role="status" className="p-4 text-sm text-muted-foreground">正在加载权限…</p>
              : error ? <div className="space-y-2 p-4">
                <p role="alert" className="text-sm text-error">{error}</p>
                <Button type="button" variant="outline" size="sm" disabled={control.disabled} onClick={() => {
                  setLoading(true)
                  setError('')
                  setAttempt((current) => current + 1)
                }}>重新加载</Button>
              </div>
                : !available.length ? <p className="p-4 text-sm text-muted-foreground">{data?.items.length ? '本页权限已全部选入' : '暂无符合条件的权限。'}</p>
                  : <ul className="divide-y">{available.map((item) => (
                    <li key={item.id}>
                      <label className={`flex cursor-pointer items-start gap-3 p-3 transition-colors hover:bg-primary/5 ${checkedLeft.includes(item.id) ? 'bg-primary/5' : ''}`} >
                        <Checkbox className="mt-0.5" checked={checkedLeft.includes(item.id)} disabled={control.disabled}
                          aria-label={`待添加 ${item.systemName} ${item.code} ${item.name}`}
                          onCheckedChange={(checked) => setCheckedLeft(check(checkedLeft, item.id, checked === true))} />
                        {permissionLabel(item.id)}
                      </label>
                    </li>
                  ))}</ul>}
          </div>
          <div className="shrink-0 space-y-2 border-t bg-muted/20 p-3 text-xs text-muted-foreground">
            <p>共 {data?.total ?? 0} 项 · 每页 10 项（含已选）</p>
            <nav aria-label="可选权限分页" className="flex flex-wrap items-center justify-between gap-2">
              <Button type="button" size="sm" variant="outline" disabled={control.disabled || loading || Boolean(error) || filters.page <= 1} onClick={() => {
                setLoading(true)
                setFilters({ ...filters, page: filters.page - 1 })
              }}>上一页</Button>
              <span aria-live="polite">{filters.page} / {pageCount}</span>
              <Button type="button" size="sm" variant="outline" disabled={control.disabled || loading || Boolean(error) || filters.page >= pageCount || filters.page >= 1000000} onClick={() => {
                setLoading(true)
                setFilters({ ...filters, page: filters.page + 1 })
              }}>下一页</Button>
            </nav>
          </div>
        </section>

        <div className="flex flex-col items-center justify-center gap-3">
          <Button type="button" size="icon" className="size-11 rounded-lg" aria-label={`添加勾选的 ${pendingAdd.length} 项权限到右侧`} title="添加到已选"
            disabled={control.disabled || !pendingAdd.length} onClick={() => {
              onChange([...new Set([...value, ...pendingAdd])])
              setCheckedLeft([])
              onBlur()
            }}><ArrowRight aria-hidden="true" /></Button>
          <Button type="button" size="icon" variant="outline" className="size-11 rounded-lg" aria-label={`移回勾选的 ${pendingRemove.length} 项权限到左侧`} title="移回可选"
            disabled={control.disabled || !pendingRemove.length} onClick={() => {
              onChange(value.filter((id) => !pendingRemove.includes(id)))
              setCheckedRight([])
              onBlur()
            }}><ArrowLeft aria-hidden="true" /></Button>
        </div>

        <section aria-label="已选权限" className="flex min-h-0 min-w-0 flex-col overflow-hidden rounded-lg border bg-card">
          <div className="shrink-0 space-y-3 border-b bg-muted/30 p-3">
            <h3 className="text-sm font-semibold" aria-live="polite">已选权限 <span className="ml-1 rounded-md bg-primary/10 px-2 py-0.5 text-xs tabular-nums text-primary">{value.length}</span></h3>
            <div className="flex flex-wrap gap-2">
              <Button type="button" size="sm" variant="outline" disabled={control.disabled || !value.length} onClick={() => setCheckedRight([...value])}>勾选全部</Button>
              <Button type="button" size="sm" variant="ghost" disabled={control.disabled || !value.length} onClick={() => {
                onChange([])
                setCheckedRight([])
                onBlur()
              }}>清空已选</Button>
            </div>
          </div>
          <div className="min-h-0 flex-1 overflow-y-auto overscroll-contain">
            {!value.length ? <p className="p-4 text-sm text-muted-foreground">暂无已选权限</p>
              : <ul className="divide-y">{value.map((id) => (
                <li key={id}>
                  <label className={`flex cursor-pointer items-start gap-3 p-3 transition-colors hover:bg-primary/5 ${checkedRight.includes(id) ? 'bg-primary/5' : ''}`} >
                    <Checkbox className="mt-0.5" checked={checkedRight.includes(id)} disabled={control.disabled}
                      aria-label={`待移回 ${labels.current.get(id)?.name ?? id}`}
                      onCheckedChange={(checked) => setCheckedRight(check(checkedRight, id, checked === true))} />
                    {permissionLabel(id)}
                  </label>
                </li>
              ))}</ul>}
          </div>
        </section>
      </div>
    </div>
  )
}
