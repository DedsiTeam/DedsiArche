import * as React from "react"
import { X } from "lucide-react"
import { cn } from "@/lib/utils"

type InputProps = React.ComponentProps<"input"> & {
  /** 文本类输入默认可清空，特殊业务字段可显式关闭。 */
  clearable?: boolean
  /** 清空并触发正常 onChange 后的附加通知。 */
  onClear?: () => void
  clearLabel?: string
  containerClassName?: string
  /** 为密码显隐等已有尾部操作预留空间。 */
  hasEndAction?: boolean
}

function Input({ className, type = "text", clearable = true, onClear, clearLabel,
  containerClassName, hasEndAction = false, ref, onChange, ...props }: InputProps) {
  const inputRef = React.useRef<HTMLInputElement>(null)
  React.useImperativeHandle(ref, () => inputRef.current!)
  const [localValue, setLocalValue] = React.useState(props.defaultValue ?? '')
  const value = props.value !== undefined ? props.value : localValue
  const supportsClear = clearable && ['text', 'search', 'tel', 'email', 'url', 'password', 'number'].includes(type)
  const canClear = supportsClear && !props.disabled && !props.readOnly && value != null && String(value).length > 0

  React.useEffect(() => {
    const input = inputRef.current
    const form = input?.form
    let timer: ReturnType<typeof setTimeout> | undefined
    function reset() {
      timer = setTimeout(() => setLocalValue(input?.value ?? ''), 0)
    }
    form?.addEventListener('reset', reset)
    return () => { form?.removeEventListener('reset', reset); clearTimeout(timer) }
  }, [])

  function clear() {
    const input = inputRef.current
    if (!input || input.matches(':disabled') || input.readOnly) return
    input.focus()
    // 使用原生 setter 和冒泡事件，让 React、RHF 与普通 onChange 走同一条更新路径。
    const setter = Object.getOwnPropertyDescriptor(HTMLInputElement.prototype, 'value')?.set
    if (!setter) return
    setter.call(input, '')
    input.dispatchEvent(new Event('input', { bubbles: true }))
    setLocalValue('')
    onClear?.()
  }

  const input = (
    <input
      {...props}
      ref={inputRef}
      type={type}
      data-slot="input"
      onChange={(event) => { setLocalValue(event.target.value); onChange?.(event) }}
      className={cn(
        "peer h-9 w-full min-w-0 rounded-md border border-input bg-card px-3 py-1 text-base shadow-xs transition-[color,box-shadow] outline-none selection:bg-primary selection:text-primary-foreground file:inline-flex file:h-7 file:border-0 file:bg-transparent file:text-sm file:font-medium file:text-foreground placeholder:text-muted-foreground disabled:pointer-events-none disabled:cursor-not-allowed disabled:opacity-50 md:text-sm",
        "focus-visible:border-primary focus-visible:ring-2 focus-visible:ring-primary/10",
        "disabled:bg-muted read-only:bg-muted aria-invalid:border-error aria-invalid:focus-visible:border-error aria-invalid:focus-visible:ring-error/10 aria-invalid:ring-error/10",
        className,
        supportsClear && (hasEndAction ? "pr-24" : "pr-10")
      )}
    />
  )
  if (!supportsClear) return input
  return (
    <div className={cn("group/clear relative w-full min-w-0", containerClassName)}>
      {input}
      {canClear && <button
        type="button"
        aria-label={clearLabel ?? `清空${props['aria-label'] ?? props.placeholder ?? '内容'}`}
        className={cn("pointer-events-none opacity-0 group-hover/clear:pointer-events-auto group-hover/clear:opacity-100 focus-visible:pointer-events-auto focus-visible:opacity-100 absolute top-1/2 flex size-8 -translate-y-1/2 items-center justify-center rounded-md text-muted-foreground hover:bg-accent hover:text-foreground focus-visible:ring-2 focus-visible:ring-ring peer-disabled:hidden", hasEndAction ? "right-12" : "right-1")}
        onMouseDown={(event) => event.preventDefault()}
        onClick={clear}
      ><X aria-hidden="true" className="size-4" /></button>}
    </div>
  )
}

export { Input }
