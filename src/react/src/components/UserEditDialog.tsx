import { useEffect, useState } from 'react'
import { isAxiosError } from 'axios'
import { Button } from '@/components/ui/button'
import { Dialog, DialogContent, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { UserFormDialog } from '@/components/UserFormDialog'
import { userDetail, type IManagedUserDetail } from '@/services/users'
import { userError } from '@/lib/api-error'

interface IUserEditDialogProps {
  id: string
  currentUserId?: string
  onClose: () => void
  onSuccess: (message: string) => void
}

/** 只有完整详情加载成功才挂载表单，避免列表摘要被当作空岗位集合保存。 */
export function UserEditDialog({ id, ...props }: IUserEditDialogProps) {
  const [user, setUser] = useState<IManagedUserDetail | null>(null)
  const [error, setError] = useState('')
  const [attempt, setAttempt] = useState(0)

  useEffect(() => {
    const request = new AbortController()
    setUser(null)
    setError('')
    userDetail(id, request.signal)
      .then((result) => { if (!request.signal.aborted) setUser(result) })
      .catch((cause: unknown) => {
        if (request.signal.aborted) return
        setError(isAxiosError(cause) && cause.response?.status === 404
          ? '该用户已不存在，请关闭弹窗并刷新列表。' : userError(cause))
      })
    return () => request.abort()
  }, [id, attempt])

  if (user) return <UserFormDialog user={user} {...props} />

  return (
    <Dialog open onOpenChange={(open) => { if (!open) props.onClose() }}>
      <DialogContent aria-describedby={undefined}>
        <DialogHeader><DialogTitle>编辑用户</DialogTitle></DialogHeader>
        {error ? <div className="space-y-3">
          <p role="alert" className="text-error">{error}</p>
          <Button type="button" variant="outline" onClick={() => {
            setError('')
            setAttempt((value) => value + 1)
          }}>重新加载</Button>
        </div> : <p role="status">正在加载用户及岗位…</p>}
      </DialogContent>
    </Dialog>
  )
}
