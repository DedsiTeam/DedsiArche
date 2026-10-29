import * as React from 'react'
import { Dialog as Primitive } from 'radix-ui'
import { X } from 'lucide-react'
import { cn } from '@/lib/utils'

function Dialog(props: React.ComponentProps<typeof Primitive.Root>) { return <Primitive.Root {...props} /> }
function DialogContent({ className, children, showCloseButton = true, ...props }: React.ComponentProps<typeof Primitive.Content> & { showCloseButton?: boolean }) {
  return <Primitive.Portal><Primitive.Overlay className="fixed inset-0 z-50 bg-black/50 data-[state=open]:animate-in data-[state=open]:fade-in-0" /><Primitive.Content className={cn('fixed left-1/2 top-1/2 z-50 grid max-h-[85vh] w-[560px] -translate-x-1/2 -translate-y-1/2 gap-6 overflow-y-auto rounded-xl border bg-background p-6 shadow-lg outline-none', className)} {...props}>{children}{showCloseButton && <Primitive.Close aria-label="关闭弹窗" className="absolute right-4 top-4 rounded-sm p-1 text-muted-foreground hover:bg-accent focus-visible:ring-2 focus-visible:ring-ring"><X className="size-4" /></Primitive.Close>}</Primitive.Content></Primitive.Portal>
}
function DialogHeader({ className, ...props }: React.ComponentProps<'div'>) { return <div className={cn('space-y-2 pr-6', className)} {...props} /> }
function DialogTitle({ className, ...props }: React.ComponentProps<typeof Primitive.Title>) { return <Primitive.Title className={cn('text-xl font-semibold', className)} {...props} /> }
function DialogDescription({ className, ...props }: React.ComponentProps<typeof Primitive.Description>) { return <Primitive.Description className={cn('text-sm leading-6 text-muted-foreground', className)} {...props} /> }
export { Dialog, DialogContent, DialogHeader, DialogTitle, DialogDescription }
