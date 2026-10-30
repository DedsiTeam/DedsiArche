import { useId, useRef, useState, type ComponentProps } from 'react'
import { Popover } from 'radix-ui'
import { ChevronDown, X } from 'lucide-react'
import { Checkbox } from './checkbox'
import type { ISelectOption } from './select'
import { cn } from '@/lib/utils'

type MultiSelectProps = Omit<ComponentProps<'button'>, 'value' | 'defaultValue' | 'onChange' | 'onBlur' | 'children'> & {
  value: string[]
  onValueChange: (value: string[]) => void
  onBlur?: () => void
  options: readonly ISelectOption[]
  placeholder?: string
  readOnly?: boolean
  clearable?: boolean
}

/** 多选绑定字符串数组；弹层选项使用真实复选框，清空为 []。 */
export function MultiSelect({ value, onValueChange, onBlur, options, placeholder = '请选择', readOnly = false, clearable = true, className, disabled, name, ...props }: MultiSelectProps) {
  const [open, setOpen] = useState(false)
  const wrapper = useRef<HTMLDivElement>(null)
  const titleId = useId()
  const summary = value.map((key) => options.find((item) => item.value === key)?.label ?? key).join('、')
  return <div ref={wrapper} className="relative w-full">
    {name && value.map((item) => <input key={item} type="hidden" name={name} value={item} disabled={disabled} />)}
    <Popover.Root open={open && !disabled && !readOnly} onOpenChange={(next) => { if (!disabled && !readOnly) setOpen(next) }}>
      <Popover.Trigger asChild><button {...props} type="button" disabled={disabled} aria-readonly={readOnly}
        onBlur={() => { if (!open) onBlur?.() }}
        className={cn('flex h-10 w-full items-center gap-2 rounded-lg border border-input bg-card pl-3 pr-20 text-left text-sm outline-none focus-visible:ring-[3px] focus-visible:ring-ring/20 disabled:bg-muted aria-readonly:bg-muted aria-invalid:border-error disabled:cursor-not-allowed disabled:opacity-50', className)}>
        <span className={cn('truncate', !value.length && 'text-muted-foreground')} title={summary}>{summary || placeholder}</span>
        <ChevronDown aria-hidden="true" className="absolute right-3 size-4 text-muted-foreground" />
      </button></Popover.Trigger>
      <Popover.Portal><Popover.Content align="start" sideOffset={6} collisionPadding={12} aria-labelledby={titleId}
        onCloseAutoFocus={() => onBlur?.()}
        className="z-[60] w-[var(--radix-popover-trigger-width)] min-w-56 rounded-lg border bg-card p-2 text-foreground shadow-lg">
        <p id={titleId} className="px-2 py-2 text-xs text-muted-foreground">已选择 {value.length} 项</p>
        <div className="max-h-64 overflow-y-auto">
          {!options.length && <p className="p-3 text-sm text-muted-foreground">暂无选项</p>}
          {options.map((option) => <label key={option.value} className={cn('flex min-h-10 items-center gap-3 rounded-md px-2 py-2 text-sm hover:bg-accent', option.disabled && 'opacity-50')}>
            <Checkbox checked={value.includes(option.value)} disabled={disabled || option.disabled} readOnly={readOnly}
              onCheckedChange={(checked) => onValueChange(checked ? [...value.filter((item) => item !== option.value), option.value] : value.filter((item) => item !== option.value))} />
            {option.label}
          </label>)}
        </div>
      </Popover.Content></Popover.Portal>
    </Popover.Root>
    {clearable && value.length > 0 && !disabled && !readOnly && <button type="button" aria-label="清空选择" className="absolute right-9 top-1/2 flex size-7 -translate-y-1/2 items-center justify-center rounded-md text-muted-foreground hover:bg-accent disabled:hidden"
      onMouseDown={(event) => event.preventDefault()} onClick={() => { wrapper.current?.querySelector<HTMLButtonElement>('[aria-haspopup]')?.focus(); onValueChange([]) }}><X aria-hidden="true" className="size-4" /></button>}
  </div>
}
