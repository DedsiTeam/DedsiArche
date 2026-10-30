import { useSyncExternalStore } from 'react'

export type MessageType = 'success' | 'warning' | 'error' | 'info'

type Message = { id: number; type: MessageType; text: string }
let messages: Message[] = []
let nextId = 0
const listeners = new Set<() => void>()
const emit = () => listeners.forEach((listener) => listener())
function show(type: Message['type'], text: string) {
  if (!text) return
  messages = [...messages.filter((item) => item.text !== text), { id: ++nextId, type, text }].slice(-3)
  emit()
}
export const notify = {
  success: (text: string) => show('success', text),
  warning: (text: string) => show('warning', text),
  error: (text: string) => show('error', text),
  info: (text: string) => show('info', text),
  dismiss: (id: number) => { messages = messages.filter((item) => item.id !== id); emit() },
}
const subscribe = (listener: () => void) => { listeners.add(listener); return () => { listeners.delete(listener) } }
export function useMessages() { return useSyncExternalStore(subscribe, () => messages) }
