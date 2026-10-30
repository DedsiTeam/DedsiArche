import { useImperativeHandle, useRef, useState, useEffect, type ComponentProps } from 'react'
import { X } from 'lucide-react'
import { cn } from '@/lib/utils'

/** 多行文本，清空触发普通 onChange，支持受控及非受控使用。 */
export function Textarea({ ref, className, clearable = true, clearLabel = '清空内容', onChange, ...props }: ComponentProps<'textarea'> & { clearable?: boolean; clearLabel?: string }) {
  const element = useRef<HTMLTextAreaElement>(null)
  useImperativeHandle(ref, () => element.current!)
  const [localValue, setLocalValue] = useState(props.defaultValue ?? '')
  const value = props.value === undefined ? localValue : props.value
  useEffect(() => {
    const form = element.current?.form
    let timer: ReturnType<typeof setTimeout> | undefined
    function reset() { timer = setTimeout(() => setLocalValue(element.current?.value ?? ''), 0) }
    form?.addEventListener('reset', reset)
    return () => { form?.removeEventListener('reset', reset); clearTimeout(timer) }
  }, [])
  return <div className="relative w-full">
    <textarea {...props} ref={element} onChange={(event) => { setLocalValue(event.target.value); onChange?.(event) }}
      className={cn('peer min-h-24 w-full resize-y rounded-lg border border-input bg-card px-3 py-2 text-sm outline-none placeholder:text-muted-foreground focus-visible:border-ring focus-visible:ring-[3px] focus-visible:ring-ring/20 disabled:bg-muted read-only:bg-muted aria-invalid:border-error aria-invalid:ring-error/20 disabled:cursor-not-allowed disabled:opacity-50', className, clearable && 'pr-11')} />
    {clearable && String(value ?? '').length > 0 && !props.disabled && !props.readOnly && <button type="button" aria-label={clearLabel}
      className="absolute right-1 top-1 flex size-8 items-center justify-center rounded-md text-muted-foreground hover:bg-accent focus-visible:ring-2 focus-visible:ring-ring peer-disabled:hidden"
      onMouseDown={(event) => event.preventDefault()} onClick={() => {
        const input = element.current
        if (!input || input.matches(':disabled') || input.readOnly) return
        input.focus()
        Object.getOwnPropertyDescriptor(HTMLTextAreaElement.prototype, 'value')?.set?.call(input, '')
        input.dispatchEvent(new Event('input', { bubbles: true }))
        setLocalValue('')
      }}><X aria-hidden="true" className="size-4" /></button>}
  </div>
}
