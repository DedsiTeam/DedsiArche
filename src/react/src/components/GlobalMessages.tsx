import { Toast } from 'radix-ui'
import { CheckCircle2, CircleAlert, Info, X } from 'lucide-react'
import { notify, useMessages } from '@/lib/notify'

export function GlobalMessages() {
  const messages = useMessages()
  return <Toast.Provider swipeDirection="up" duration={3500}>
    {messages.map((item) => {
      const Icon = item.type === 'success' ? CheckCircle2 : item.type === 'error' ? CircleAlert : Info
      return <Toast.Root key={item.id} open duration={item.type === 'error' ? 5000 : 3500} onOpenChange={(open) => { if (!open) notify.dismiss(item.id) }} type={item.type === 'error' ? 'foreground' : 'background'} className="pointer-events-auto flex w-fit max-w-[560px] items-center gap-3 rounded-xl border bg-white px-4 py-3 text-foreground shadow-lg">
        <Icon className="size-5 shrink-0 text-primary" aria-hidden="true" />
        <Toast.Title className="break-words text-sm leading-6">{item.text}</Toast.Title>
        <Toast.Close aria-label="关闭提示" className="shrink-0 rounded p-1 text-muted-foreground hover:bg-accent focus-visible:ring-2 focus-visible:ring-ring"><X className="size-4" /></Toast.Close>
      </Toast.Root>
    })}
    <Toast.Viewport label="全局提示" className="pointer-events-none fixed left-1/2 top-5 z-[100] flex max-w-[calc(100vw-4rem)] -translate-x-1/2 flex-col items-center gap-2 outline-none" />
  </Toast.Provider>
}
