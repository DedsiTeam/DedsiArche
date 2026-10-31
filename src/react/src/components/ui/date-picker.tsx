import { useId, useRef, useState, type ComponentProps, type KeyboardEvent } from 'react'
import { Popover } from 'radix-ui'
import { CalendarDays, ChevronLeft, ChevronRight, X } from 'lucide-react'
import { Select } from './select'
import { Button } from './button'
import { cn } from '@/lib/utils'

/** 本地日期字符串，不经过 UTC 转换，避免时区导致日期偏移。 */
export function parseDate(value: string): Date | undefined {
  if (!/^\d{4}-\d{2}-\d{2}$/.test(value)) return undefined
  const [year, month, day] = value.split('-').map(Number)
  if (year < 1 || month < 1 || month > 12 || day < 1 || day > 31) return undefined
  const date = new Date(0)
  date.setHours(12, 0, 0, 0)
  date.setFullYear(year, month - 1, day)
  return date.getFullYear() === year && date.getMonth() === month - 1 && date.getDate() === day ? date : undefined
}
function formatDate(date: Date) {
  return `${String(date.getFullYear()).padStart(4, '0')}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
}

type DatePickerProps = Omit<ComponentProps<'button'>, 'value' | 'defaultValue' | 'onChange' | 'onBlur' | 'children'> & {
  value: string
  onValueChange: (value: string) => void
  onBlur?: () => void
  min?: string
  max?: string
  placeholder?: string
  readOnly?: boolean
  clearable?: boolean
}

export function DatePicker({ value, onValueChange, onBlur, min, max, placeholder = '请选择日期', readOnly = false, clearable = true, className, disabled, name, ...props }: DatePickerProps) {
  const lower = parseDate(min ?? '') ? min! : '0001-01-01'
  const upper = parseDate(max ?? '') ? max! : '9999-12-31'
  const clamp = (date: string) => date < lower ? lower : date > upper ? upper : date
  const initial = clamp(parseDate(value) ? value : formatDate(new Date()))
  const [cursor, setCursor] = useState(initial)
  const [open, setOpen] = useState(false)
  const wrapper = useRef<HTMLDivElement>(null)
  const calendar = useRef<HTMLDivElement>(null)
  const titleId = useId()
  const displayed = parseDate(cursor)!
  const year = displayed.getFullYear()
  const month = displayed.getMonth()
  const first = new Date(displayed); first.setDate(1)
  const days = new Date(displayed); days.setMonth(month + 1, 0)
  const today = formatDate(new Date())
  const firstYear = min ? parseDate(min)?.getFullYear() ?? 1900 : Math.min(1900, year)
  const lastYear = max ? parseDate(max)?.getFullYear() ?? 2100 : Math.max(2100, year)
  const empty = lower > upper
  function move(date: Date, focus = false) {
    const next = date.getFullYear() < 1 ? lower : date.getFullYear() > 9999 ? upper : clamp(formatDate(date))
    setCursor(next)
    if (focus) requestAnimationFrame(() => calendar.current?.querySelector<HTMLButtonElement>(`[data-date="${next}"]`)?.focus())
  }
  function changeMonth(delta: number) { const next = new Date(displayed); next.setDate(1); next.setMonth(month + delta); move(next) }
  function keyDown(event: KeyboardEvent<HTMLButtonElement>, date: Date) {
    const delta: Record<string, number> = { ArrowLeft: -1, ArrowRight: 1, ArrowUp: -7, ArrowDown: 7, Home: -((date.getDay() + 6) % 7), End: 6 - ((date.getDay() + 6) % 7) }
    const next = new Date(date)
    if (event.key in delta) next.setDate(date.getDate() + delta[event.key])
    else if (event.key === 'PageUp' || event.key === 'PageDown') { next.setDate(1); next.setMonth(next.getMonth() + (event.key === 'PageUp' ? -1 : 1)) }
    else return
    event.preventDefault(); move(next, true)
  }
  return <div ref={wrapper} className="group/clear relative w-full">
    {name && <input type="hidden" name={name} value={value} disabled={disabled} />}
    <Popover.Root open={open && !disabled && !readOnly} onOpenChange={(next) => { if (!disabled && !readOnly) { if (next) setCursor(initial); setOpen(next) } }}>
      <Popover.Trigger asChild><button {...props} type="button" disabled={disabled} aria-readonly={readOnly} onBlur={() => { if (!open) onBlur?.() }}
        className={cn('flex h-10 w-full items-center rounded-lg border border-input bg-card pl-3 pr-20 text-left text-sm outline-none focus-visible:border-primary focus-visible:ring-2 focus-visible:ring-primary/10 disabled:bg-muted aria-readonly:bg-muted aria-invalid:border-error aria-invalid:focus-visible:border-error aria-invalid:focus-visible:ring-error/10 disabled:cursor-not-allowed disabled:opacity-50', className)}>
        <span className={cn('truncate', !value && 'text-muted-foreground')}>{value || placeholder}</span><CalendarDays aria-hidden="true" className="absolute right-3 size-4 text-muted-foreground" />
      </button></Popover.Trigger>
      <Popover.Portal><Popover.Content ref={calendar} align="start" sideOffset={6} collisionPadding={12} aria-labelledby={titleId}
        onOpenAutoFocus={(event) => { event.preventDefault(); requestAnimationFrame(() => calendar.current?.querySelector<HTMLButtonElement>(`[data-date="${cursor}"]`)?.focus()) }} onCloseAutoFocus={() => onBlur?.()}
        className="z-[60] w-80 max-h-[var(--radix-popover-content-available-height)] overflow-y-auto rounded-lg border bg-card p-3 text-foreground shadow-lg">
        <p id={titleId} className="sr-only" aria-live="polite">{year}年{month + 1}月</p>
        <div className="mb-3 flex items-center gap-1">
          <Button type="button" variant="ghost" size="icon-sm" aria-label="上个月" disabled={empty || cursor.slice(0, 7) <= lower.slice(0, 7)} onClick={() => changeMonth(-1)}><ChevronLeft /></Button>
          <Select aria-label="年份" className="min-w-0 flex-1 [&_button]:min-w-0" value={String(year)} clearable={false} onValueChange={(next) => { const date = new Date(displayed); date.setDate(1); date.setFullYear(Number(next)); move(date) }} options={Array.from({ length: Math.max(0, lastYear - firstYear + 1) }, (_, index) => ({ value: String(firstYear + index), label: `${firstYear + index}年` }))} />
          <Select aria-label="月份" className="min-w-0 w-20 [&_button]:min-w-0" value={String(month)} clearable={false} onValueChange={(next) => { const date = new Date(displayed); date.setDate(1); date.setMonth(Number(next)); move(date) }} options={Array.from({ length: 12 }, (_, index) => ({ value: String(index), label: `${index + 1}月` }))} />
          <Button type="button" variant="ghost" size="icon-sm" aria-label="下个月" disabled={empty || cursor.slice(0, 7) >= upper.slice(0, 7)} onClick={() => changeMonth(1)}><ChevronRight /></Button>
        </div>
        <div className="grid grid-cols-7 text-center text-xs text-muted-foreground" aria-hidden="true">{['一','二','三','四','五','六','日'].map((day) => <span key={day} className="py-2">{day}</span>)}</div>
        <div className="grid grid-cols-7 gap-1">
          {Array.from({ length: (first.getDay() + 6) % 7 }, (_, index) => <span key={`blank-${index}`} />)}
          {Array.from({ length: days.getDate() }, (_, index) => {
            const date = new Date(first); date.setDate(index + 1)
            const key = formatDate(date)
            return <button key={key} data-date={key} type="button" tabIndex={key === cursor ? 0 : -1} disabled={empty || key < lower || key > upper}
              aria-label={`${year}年${month + 1}月${index + 1}日`} aria-pressed={key === value} aria-current={key === today ? 'date' : undefined}
              onFocus={() => setCursor(key)} onKeyDown={(event) => keyDown(event, date)} onClick={() => { if (!disabled && !readOnly) { onValueChange(key); setOpen(false) } }}
              className={cn('h-9 rounded-md text-sm outline-none hover:bg-accent focus-visible:ring-2 focus-visible:ring-ring disabled:cursor-not-allowed disabled:opacity-30', key === today && 'border border-primary', key === value && 'bg-primary text-primary-foreground hover:bg-primary/90')}>{index + 1}</button>
          })}
        </div>
        <div className="mt-3 border-t pt-2"><Button type="button" variant="ghost" size="sm" disabled={empty || today < lower || today > upper} onClick={() => { onValueChange(today); setOpen(false) }}>今天</Button></div>
      </Popover.Content></Popover.Portal>
    </Popover.Root>
    {clearable && value && !disabled && !readOnly && <button type="button" aria-label="清空日期" className="pointer-events-none opacity-0 group-hover/clear:pointer-events-auto group-hover/clear:opacity-100 focus-visible:pointer-events-auto focus-visible:opacity-100 focus-visible:ring-2 focus-visible:ring-ring absolute right-9 top-1/2 flex size-7 -translate-y-1/2 items-center justify-center rounded-md text-muted-foreground hover:bg-accent disabled:hidden"
      onMouseDown={(event) => event.preventDefault()} onClick={() => { wrapper.current?.querySelector<HTMLButtonElement>('[aria-haspopup]')?.focus(); onValueChange('') }}><X aria-hidden="true" className="size-4" /></button>}
  </div>
}
