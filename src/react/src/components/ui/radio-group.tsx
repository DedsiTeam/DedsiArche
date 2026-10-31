import { useRef, type ComponentProps } from 'react'
import { RadioGroup as Primitive } from 'radix-ui'
import { X } from 'lucide-react'
import type { ISelectOption } from './select'
import { cn } from '@/lib/utils'

type RadioGroupProps = Omit<ComponentProps<typeof Primitive.Root>, 'value' | 'onValueChange'> & {
  value: string
  onValueChange: (value: string) => void
  options: readonly ISelectOption[]
  readOnly?: boolean
  clearable?: boolean
}

/** 字符串单选组，方向键切换，空字符串表示未选。 */
export function RadioGroup({ options, className, value, onValueChange, readOnly = false, clearable = true, ...props }: RadioGroupProps) {
  const wrapper = useRef<HTMLDivElement>(null)
  return <div ref={wrapper} className="group/clear flex items-start gap-2">
    <Primitive.Root {...props} tabIndex={-1} disabled={props.disabled || readOnly} value={value} onValueChange={(next) => { if (!readOnly) onValueChange(next) }} aria-readonly={readOnly}
      className={cn('flex min-h-9 flex-1 flex-wrap items-center gap-x-5 gap-y-3 rounded-lg outline-none focus-visible:ring-2 focus-visible:ring-ring', className)}>
      {options.map((option) => <label key={option.value} className={cn('inline-flex min-h-9 items-center gap-2 text-sm', (props.disabled || option.disabled) && 'opacity-50')}>
        <Primitive.Item value={option.value} disabled={props.disabled || readOnly || option.disabled} aria-invalid={props['aria-invalid']}
          className="flex size-5 shrink-0 items-center justify-center rounded-full border border-input bg-card outline-none data-[state=checked]:border-primary focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 aria-invalid:border-error disabled:cursor-not-allowed">
          <Primitive.Indicator className="size-2.5 rounded-full bg-primary" />
        </Primitive.Item>{option.label}
      </label>)}
    </Primitive.Root>
    {clearable && value && !props.disabled && !readOnly && <button type="button" aria-label="清空选择" className="pointer-events-none opacity-0 group-hover/clear:pointer-events-auto group-hover/clear:opacity-100 focus-visible:pointer-events-auto focus-visible:opacity-100 focus-visible:ring-2 focus-visible:ring-ring flex size-8 items-center justify-center rounded-md text-muted-foreground hover:bg-accent disabled:hidden"
      onClick={() => { onValueChange(''); wrapper.current?.querySelector<HTMLButtonElement>('[role=radio]:not(:disabled)')?.focus() }}><X aria-hidden="true" className="size-4" /></button>}
  </div>
}
