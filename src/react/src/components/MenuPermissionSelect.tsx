import { useEffect, useState, type Ref } from 'react'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import type { FormControlProps } from '@/components/ui/form'
import { listPermissions, type IPermission, type IPermissionPage } from '@/services/permissions'
import { permissionError } from '@/lib/permission-validation'

interface IMenuPermissionSelectProps extends Partial<FormControlProps> {
  systemCode: string
  value: string
  initialLabel?: string
  onChange: (id: string) => void
  onBlur?: () => void
  inputRef?: Ref<HTMLButtonElement>
}

/** 同系统权限单选；搜索翻页不替换已选值，清空才显式解除关联。 */
export function MenuPermissionSelect({ systemCode, value, initialLabel, onChange, onBlur, inputRef, ...control }: IMenuPermissionSelectProps) {
  const [keyword, setKeyword] = useState('')
  const [query, setQuery] = useState({ keyword: '', page: 1 })
  const [data, setData] = useState<IPermissionPage | null>(null)
  const [selected, setSelected] = useState<IPermission | null>(null)
  const [loadedSystem, setLoadedSystem] = useState('')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [attempt, setAttempt] = useState(0)
  useEffect(() => {
    const controller = new AbortController()
    setData(null)
    setError('')
    if (!systemCode) {
      setLoading(false)
      return () => controller.abort()
    }
    setLoading(true)
    listPermissions({ ...query, systemCode, status: '' }, controller.signal)
      .then((result) => {
        if (controller.signal.aborted) return
        setData(result)
        setLoadedSystem(systemCode)
      })
      .catch((cause: unknown) => {
        if (!controller.signal.aborted) setError(permissionError(cause))
      })
      .finally(() => {
        if (!controller.signal.aborted) setLoading(false)
      })
    return () => controller.abort()
  }, [systemCode, query, attempt])

  const waiting = loading || loadedSystem !== systemCode
  const label = selected?.id === value ? `${selected.code} · ${selected.name}` : initialLabel || value
  return (
    <div className="space-y-3 rounded-lg border p-3" aria-busy={loading}>
      <div className="flex items-center justify-between gap-2">
        <p className="min-w-0 break-all text-sm">{value ? `已选：${label}` : '未绑定权限'}</p>
        <Button {...control} ref={inputRef} onBlur={onBlur} type="button" size="sm" variant="outline" onClick={() => onChange('')}>
          清空
        </Button>
      </div>
      <div className="flex gap-2">
        <Input
          aria-label="搜索同系统权限"
          placeholder="权限编码或名称"
          maxLength={100}
          value={keyword}
          disabled={control.disabled || !systemCode}
          onChange={(event) => setKeyword(event.target.value)}
          onKeyDown={(event) => {
            if (event.key === 'Enter') {
              event.preventDefault()
              if (!control.disabled) setQuery({ keyword: keyword.trim(), page: 1 })
            }
          }}
        />
        <Button type="button" size="sm" disabled={control.disabled || !systemCode || loading} onClick={() => setQuery({ keyword: keyword.trim(), page: 1 })}>搜索</Button>
      </div>
      {!systemCode ? <p className="text-sm text-muted-foreground">请先选择所属系统。</p>
        : error ? <div role="alert" className="text-sm text-error">
          {error}<Button type="button" variant="link" disabled={control.disabled} onClick={() => setAttempt((item) => item + 1)}>重试</Button>
        </div>
          : waiting ? <p role="status" className="text-sm text-muted-foreground">正在加载权限…</p>
            : <div className="max-h-44 space-y-1 overflow-y-auto">
              {!data?.items.length && <p className="text-sm text-muted-foreground">暂无符合条件的权限。</p>}
              {data?.items.map((item) => (
                <Button
                  key={item.id}
                  type="button"
                  variant={value === item.id ? 'secondary' : 'ghost'}
                  className="h-auto w-full justify-start whitespace-normal break-all px-2 py-2 text-left"
                  disabled={control.disabled || item.systemCode !== systemCode}
                  aria-pressed={value === item.id}
                  onClick={() => {
                    setSelected(item)
                    onChange(item.id)
                    onBlur?.()
                  }}
                >
                  {item.code} · {item.name}{item.enabled ? '' : '（已停用）'}
                </Button>
              ))}
            </div>}
      <div className="flex items-center justify-end gap-2 text-xs text-muted-foreground">
        <span>第 {query.page} 页 · 共 {data?.total ?? 0} 项</span>
        <Button type="button" size="sm" variant="outline" disabled={control.disabled || waiting || Boolean(error) || query.page <= 1} onClick={() => setQuery({ ...query, page: query.page - 1 })}>上一页</Button>
        <Button type="button" size="sm" variant="outline" disabled={control.disabled || waiting || Boolean(error) || !data || query.page * data.size >= data.total} onClick={() => setQuery({ ...query, page: query.page + 1 })}>下一页</Button>
      </div>
    </div>
  )
}
