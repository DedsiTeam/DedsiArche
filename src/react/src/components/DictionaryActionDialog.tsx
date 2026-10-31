import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { useEffect, useRef, useState } from 'react'
import { Button } from '@/components/ui/button'
import { Dialog, DialogContent, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { DictionaryFormDialog } from '@/components/DictionaryFormDialog'
import {
  deleteDictionary, dictionaryDetail,
  type IDictionary,
} from '@/services/dictionaries'
import { dictionaryError } from '@/lib/dictionary-validation'
import { notify } from '@/lib/notify'

export type DictionaryAction = { kind: 'detail' | 'edit' | 'delete'; id: string }

interface IDictionaryActionDialogProps {
  action: DictionaryAction
  onClose: () => void
  onSuccess: (message: string) => void
}

/** 详情、编辑及删除均先读取最新记录；写入仍使用版本校验。 */
export function DictionaryActionDialog({ action, onClose, onSuccess }: IDictionaryActionDialogProps) {
  const [dictionary, setDictionary] = useState<IDictionary | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [attempt, setAttempt] = useState(0)
  const [pending, setPending] = useState(false)
  const lock = useRef(false)
  const titles = { detail: '字典详情', edit: '编辑字典', delete: '确认删除字典' }

  useEffect(() => {
    const controller = new AbortController()
    setDictionary(null)
    setLoading(true)
    setError('')
    dictionaryDetail(action.id, controller.signal)
      .then((result) => { if (!controller.signal.aborted) setDictionary(result) })
      .catch((cause: unknown) => {
        if (!controller.signal.aborted) setError(dictionaryError(cause))
      })
      .finally(() => { if (!controller.signal.aborted) setLoading(false) })
    return () => controller.abort()
  }, [action.id, attempt])

  async function remove() {
    if (!dictionary || lock.current) return
    lock.current = true
    setPending(true)
    try {
      await deleteDictionary(dictionary.id, dictionary.version)
      onSuccess(`字典 ${dictionary.name} 已删除。`)
    } catch (cause) {
      notify.error(dictionaryError(cause, 'delete'))
    } finally {
      lock.current = false
      setPending(false)
    }
  }

  if (!loading && !error && dictionary && action.kind === 'edit') {
    return <DictionaryFormDialog dictionary={dictionary} onClose={onClose} onSuccess={onSuccess} />
  }

  const details = dictionary ? [
    ['字典 ID', dictionary.id],
    ['字典编码', dictionary.code],
    ['字典名称', dictionary.name],
    ['所属系统编码', dictionary.systemCode ?? '未设置'],
    ['所属系统名称', dictionary.systemName ?? '未设置'],
    ['字典状态', dictionary.enabled ? '已启用' : '已停用'],
    ['字典说明', dictionary.description || '—'],
    ['创建时间', dictionary.createdAt.replace('T', ' ').slice(0, 19)],
    ['修改时间', dictionary.updatedAt.replace('T', ' ').slice(0, 19)],
  ] : []

  return (
    <Dialog open onOpenChange={(open) => { if (!open && !pending) onClose() }}>
      <DialogContent className={action.kind === 'detail'
        ? 'flex h-[82vh] max-h-[760px] w-[calc(100vw-48px)] max-w-[1280px] flex-col gap-5 overflow-hidden bg-card'
        : 'w-[calc(100vw-48px)] max-w-4xl'} aria-describedby={undefined} showCloseButton={!pending}>
        <DialogHeader className={action.kind === 'detail' ? 'shrink-0 border-b pb-4' : undefined}><DialogTitle>{titles[action.kind]}</DialogTitle></DialogHeader>
        {loading ? <p role="status">正在加载字典…</p> : error ? (
          <div className="space-y-3">
            <p role="alert" className="text-sm text-error">{error}</p>
            <Button variant="outline" onClick={() => {
              setError('')
              setLoading(true)
              setAttempt((value) => value + 1)
            }}>重新加载</Button>
          </div>
        ) : dictionary && (
          action.kind === 'detail' ? (
            <div className="grid min-h-0 flex-1 grid-cols-[minmax(260px,0.85fr)_minmax(0,2fr)] gap-5">
              <section aria-label="基本信息" className="min-w-0 space-y-4 overflow-y-auto overscroll-contain py-1 pr-4">
                <h2 className="text-sm font-semibold">基本信息</h2>
                <dl className="grid grid-cols-[100px_minmax(0,1fr)] gap-x-3 gap-y-4 text-sm">
                  {details.map(([label, value]) => (
                    <div key={label} className="contents">
                      <dt className="text-muted-foreground">{label}</dt>
                      <dd className="whitespace-pre-wrap break-all">{value}</dd>
                    </div>
                  ))}
                </dl>
              </section>
              <section aria-label="字典项" className="min-w-0 space-y-3 overflow-y-auto overscroll-contain border-l py-1 pl-5">
                <h3 className="text-sm font-semibold">字典项 <span className="ml-1 rounded-md bg-primary/10 px-2 py-0.5 text-xs tabular-nums text-primary">{dictionary.items.length}</span></h3>
                {dictionary.items.length === 0 ? <p className="text-sm text-muted-foreground">此字典尚未配置字典项。</p> : (
                  <div className="overflow-hidden rounded-lg border bg-card">
                    <Table className="w-full table-auto [&_th]:whitespace-nowrap [&_td]:whitespace-nowrap">
                      <TableHeader className="bg-muted/30"><TableRow>
                        <TableHead>字典项编码</TableHead><TableHead>字典项名称</TableHead>
                        <TableHead>排序</TableHead><TableHead>默认项</TableHead><TableHead>状态</TableHead>
                      </TableRow></TableHeader>
                      <TableBody>{dictionary.items.map((item) => (
                        <TableRow key={item.id} className="even:bg-muted/20 hover:bg-primary/5">
                          <TableCell className="font-mono text-xs">{item.code}</TableCell>
                          <TableCell className="max-w-56 truncate" title={item.name}>{item.name}</TableCell>
                          <TableCell className="tabular-nums">{item.sortOrder}</TableCell>
                          <TableCell>{item.isDefault ? '是' : '否'}</TableCell>
                          <TableCell>{item.enabled ? '已启用' : '已停用'}</TableCell>
                        </TableRow>
                      ))}</TableBody>
                    </Table>
                  </div>
                )}
              </section>
            </div>
          ) : (
            <div className="space-y-4" aria-busy={pending}>
              <div className="rounded-lg bg-secondary p-4 text-sm">
                <p className="break-all font-medium">{dictionary.name}</p>
                <p className="mt-1 break-all text-muted-foreground">{dictionary.code}</p>
              </div>
              <p className="text-sm">确定删除此字典吗？删除后无法恢复，字典及其全部字典项将一并删除。</p>
            </div>
          )
        )}
        <div className={action.kind === 'detail'
          ? 'mt-auto flex shrink-0 justify-end gap-3 border-t pt-4'
          : 'flex justify-end gap-3'}>
          <Button type="button" variant="outline" disabled={pending} onClick={onClose}>
            {action.kind === 'delete' ? '取消' : '关闭'}
          </Button>
          {action.kind === 'delete' && (
            <Button type="button" variant="destructive" disabled={loading || pending || Boolean(error) || !dictionary} onClick={() => void remove()}>
              {pending ? '正在删除…' : '确认删除'}
            </Button>
          )}
        </div>
      </DialogContent>
    </Dialog>
  )
}
