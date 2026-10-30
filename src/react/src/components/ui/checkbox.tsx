import type { ComponentProps } from 'react'
import { Checkbox as Primitive } from 'radix-ui'
import { Check, Minus } from 'lucide-react'
import { cn } from '@/lib/utils'

/** 布尔/半选复选框，保留原生表单语义与键盘交互。 */
export function Checkbox({ className, readOnly = false, onCheckedChange, ...props }: ComponentProps<typeof Primitive.Root> & { readOnly?: boolean }) {
  return <Primitive.Root {...props} disabled={props.disabled || readOnly} aria-readonly={readOnly} onCheckedChange={(value) => { if (!readOnly) onCheckedChange?.(value) }}
    className={cn('group inline-flex size-5 shrink-0 items-center justify-center rounded border border-input bg-card text-primary-foreground outline-none data-[state=checked]:border-primary data-[state=checked]:bg-primary data-[state=indeterminate]:border-primary data-[state=indeterminate]:bg-primary focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 aria-invalid:border-error aria-invalid:ring-error/20 disabled:cursor-not-allowed disabled:opacity-50', className)}>
    <Primitive.Indicator><Minus aria-hidden="true" className="hidden size-3.5 group-data-[state=indeterminate]:block" /><Check aria-hidden="true" className="size-3.5 group-data-[state=indeterminate]:hidden" /></Primitive.Indicator>
  </Primitive.Root>
}
