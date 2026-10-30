import { Toast } from 'radix-ui'
import { CheckCircle2, CircleX, Info, TriangleAlert, X } from 'lucide-react'
import { notify, useMessages, type MessageType } from '@/lib/notify'
import { cn } from '@/lib/utils'

// 状态同时通过图标、语义颜色和读屏标签表达，避免只依赖颜色。
const messageVariants = {
  success: {
    icon: CheckCircle2,
    label: '成功',
    className: 'border-success-border bg-success-surface text-success',
    duration: 3500,
  },
  warning: {
    icon: TriangleAlert,
    label: '警告',
    className: 'border-warning-border bg-warning-surface text-warning',
    duration: 5000,
  },
  error: {
    icon: CircleX,
    label: '错误',
    className: 'border-error-border bg-error-surface text-error',
    duration: 5000,
  },
  info: {
    icon: Info,
    label: '提示',
    className: 'border-info-border bg-info-surface text-info',
    duration: 3500,
  },
} satisfies Record<MessageType, {
  icon: typeof Info
  label: string
  className: string
  duration: number
}>

export function GlobalMessages() {
  const messages = useMessages()

  return (
    <Toast.Provider swipeDirection="up" duration={3500}>
      {messages.map((item) => {
        const variant = messageVariants[item.type]
        const Icon = variant.icon

        return (
          <Toast.Root
            key={item.id}
            open
            duration={variant.duration}
            onOpenChange={(open) => {
              if (!open) notify.dismiss(item.id)
            }}
            type={item.type === 'error' || item.type === 'warning' ? 'foreground' : 'background'}
            className={cn(
              'pointer-events-auto flex w-fit max-w-[560px] items-center gap-3 rounded-xl border px-4 py-3 shadow-lg',
              variant.className,
            )}
          >
            <Icon className="size-5 shrink-0" aria-hidden="true" />
            <Toast.Title className="min-w-0 break-words text-sm leading-6">
              <span className="sr-only">{variant.label}：</span>
              {item.text}
            </Toast.Title>
            <Toast.Close
              aria-label="关闭提示"
              className="shrink-0 rounded p-1 text-current transition-colors hover:bg-current/10 focus-visible:ring-2 focus-visible:ring-current"
            >
              <X className="size-4" aria-hidden="true" />
            </Toast.Close>
          </Toast.Root>
        )
      })}
      <Toast.Viewport
        label="全局提示"
        className="pointer-events-none fixed left-1/2 top-5 z-[100] flex max-w-[calc(100vw-4rem)] -translate-x-1/2 flex-col items-center gap-2 outline-none"
      />
    </Toast.Provider>
  )
}
