import { useEffect, useState, type Ref } from 'react'
import { Button } from '@/components/ui/button'
import { Select } from '@/components/ui/select'
import type { FormControlProps } from '@/components/ui/form'
import { listManagedSystemOptions } from '@/services/managed-systems'
import { managedSystemError } from '@/lib/managed-system-validation'

interface IPermissionSystemSelectProps extends Partial<FormControlProps> {
  value: string
  onChange: (code: string) => void
  onBlur?: () => void
  inputRef?: Ref<HTMLButtonElement>
  /* 列表筛选以空值表示全部系统，表单空值仍需必填校验。 */
  filter?: boolean
  className?: string
}

/** 仅展示下拉选择，通过专用接口读取系统编码和名称。 */
export function PermissionSystemSelect({ value, onChange, onBlur, inputRef, filter = false, className, ...control }: IPermissionSystemSelectProps) {
  const [options, setOptions] = useState<{ value: string; label: string }[]>([])
  const [attempt, setAttempt] = useState(0)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    const request = new AbortController()
    listManagedSystemOptions(request.signal)
      .then((systems) => {
        if (!request.signal.aborted) {
          setOptions(systems.map((system) => ({
            value: system.code,
            label: system.name,
          })))
        }
      })
      .catch((cause: unknown) => {
        if (!request.signal.aborted) setError(managedSystemError(cause))
      })
      .finally(() => { if (!request.signal.aborted) setLoading(false) })
    return () => request.abort()
  }, [attempt])

  return (
    <div className={className ? `space-y-2 ${className}` : 'space-y-2'} aria-busy={loading}>
      <Select
        {...control}
        ref={inputRef}
        className="w-full"
        aria-label={filter ? '筛选所属系统' : undefined}
        clearLabel={filter ? '清空所属系统筛选' : '清空所属系统'}
        value={value}
        disabled={control.disabled || loading || Boolean(error)}
        onBlur={onBlur}
        options={options}
        placeholder={loading ? '正在加载系统…' : filter ? '全部系统' : options.length ? '请选择所属系统' : '暂无系统，请先在系统管理中登记'}
        onValueChange={onChange}
      />
      {error && (
        <p role="alert" className="text-sm text-error">
          {error}
          <Button type="button" variant="link" disabled={control.disabled} onClick={() => {
            setError('')
            setLoading(true)
            setAttempt((value) => value + 1)
          }}>重试</Button>
        </p>
      )}
    </div>
  )
}
