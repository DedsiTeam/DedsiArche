import type { ComponentProps, ReactNode } from 'react'
import { Select as SelectPrimitive } from 'radix-ui'
import { Check, ChevronDown, ChevronUp } from 'lucide-react'
import { cn } from '@/lib/utils'

export interface SelectOption {
  /** Radix Select 的选项值必须为非空字符串。 */
  value: string
  label: string
  disabled?: boolean
}

type SelectProps = Omit<ComponentProps<typeof SelectPrimitive.Root>, 'children'> &
  Pick<ComponentProps<typeof SelectPrimitive.Trigger>, 'id' | 'aria-label' | 'aria-labelledby' | 'aria-describedby' | 'aria-invalid'> & {
    options: readonly SelectOption[]
    placeholder?: ReactNode
    className?: string
  }

/** 项目统一单选下拉框，支持键盘导航、禁用项及 Modal 内使用。 */
export function Select({
  options, placeholder = '请选择', className, id,
  'aria-label': ariaLabel, 'aria-labelledby': ariaLabelledBy,
  'aria-describedby': ariaDescribedBy, 'aria-invalid': ariaInvalid,
  ...props
}: SelectProps) {
  return <SelectPrimitive.Root {...props}>
    <SelectPrimitive.Trigger
      id={id} aria-label={ariaLabel} aria-labelledby={ariaLabelledBy}
      aria-describedby={ariaDescribedBy} aria-invalid={ariaInvalid}
      data-slot="select-trigger"
      className={cn('group inline-flex h-10 min-w-36 items-center justify-between gap-3 rounded-lg border border-input bg-white px-3 text-sm text-foreground shadow-xs outline-none transition-[border-color,box-shadow] hover:border-black/30 focus-visible:border-primary focus-visible:ring-[3px] focus-visible:ring-primary/20 data-[state=open]:border-primary data-[state=open]:ring-[3px] data-[state=open]:ring-primary/10 data-[placeholder]:text-muted-foreground disabled:cursor-not-allowed disabled:opacity-50 [&>span:first-child]:truncate', className)}>
      <SelectPrimitive.Value placeholder={placeholder} />
      <SelectPrimitive.Icon asChild><ChevronDown className="size-4 shrink-0 text-muted-foreground transition-transform group-data-[state=open]:rotate-180" /></SelectPrimitive.Icon>
    </SelectPrimitive.Trigger>
    <SelectPrimitive.Portal>
      <SelectPrimitive.Content position="popper" align="start" sideOffset={6} collisionPadding={12} data-slot="select-content"
        className="z-[60] max-h-[min(320px,var(--radix-select-content-available-height))] min-w-[var(--radix-select-trigger-width)] overflow-hidden rounded-xl border bg-white text-foreground shadow-lg data-[state=open]:animate-in data-[state=open]:fade-in-0 data-[state=open]:zoom-in-95 data-[state=closed]:animate-out data-[state=closed]:fade-out-0">
        <SelectPrimitive.ScrollUpButton className="flex h-7 items-center justify-center text-muted-foreground"><ChevronUp className="size-4" /></SelectPrimitive.ScrollUpButton>
        <SelectPrimitive.Viewport className="p-1.5">
          {options.map((option) => <SelectPrimitive.Item key={option.value} value={option.value} disabled={option.disabled}
            className="relative flex min-h-9 cursor-pointer select-none items-center rounded-md py-2 pl-3 pr-9 text-sm outline-none data-[highlighted]:bg-accent data-[state=checked]:bg-primary/10 data-[state=checked]:font-medium data-[disabled]:pointer-events-none data-[disabled]:opacity-40">
            <SelectPrimitive.ItemText>{option.label}</SelectPrimitive.ItemText>
            <SelectPrimitive.ItemIndicator className="absolute right-3 inline-flex items-center text-primary"><Check className="size-4" /></SelectPrimitive.ItemIndicator>
          </SelectPrimitive.Item>)}
        </SelectPrimitive.Viewport>
        <SelectPrimitive.ScrollDownButton className="flex h-7 items-center justify-center text-muted-foreground"><ChevronDown className="size-4" /></SelectPrimitive.ScrollDownButton>
      </SelectPrimitive.Content>
    </SelectPrimitive.Portal>
  </SelectPrimitive.Root>
}
