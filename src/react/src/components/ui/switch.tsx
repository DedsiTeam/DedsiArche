import type { ComponentProps } from 'react'
import { Switch as Primitive } from 'radix-ui'
import { cn } from '@/lib/utils'

/** 开关仅绑定 boolean；关闭是合法状态，非空业务规则由 Form 配置。 */
export function Switch({ className, readOnly = false, onCheckedChange, ...props }: ComponentProps<typeof Primitive.Root> & { readOnly?: boolean }) {
  return <Primitive.Root {...props} disabled={props.disabled || readOnly} aria-readonly={readOnly} onCheckedChange={(value) => { if (!readOnly) onCheckedChange?.(value) }}
    className={cn('inline-flex h-6 w-11 shrink-0 items-center rounded-full border border-input bg-secondary p-0.5 outline-none transition-colors data-[state=checked]:border-primary data-[state=checked]:bg-primary focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 aria-invalid:border-error disabled:cursor-not-allowed disabled:opacity-50', className)}>
    <Primitive.Thumb className="pointer-events-none block size-4 rounded-full bg-card shadow-sm transition-transform data-[state=checked]:translate-x-5" />
  </Primitive.Root>
}
