import { Navigate, Outlet, useLocation } from 'react-router-dom'
import { useEffect, useState } from 'react'
import { getProfile, hasUserProfile, useSession } from '@/services/auth'
import { Button } from '@/components/ui/button'

export function RequireAuth() {
  const session = useSession()
  const location = useLocation()
  const [error, setError] = useState('')
  const [attempt, setAttempt] = useState(0)
  const ready = hasUserProfile(session?.user)
  const token = session?.accessToken

  useEffect(() => {
    if (!token || ready) return
    let active = true
    setError('')
    // 不在卸载时取消共享请求；仅忽略当前订阅者的回调。
    getProfile().catch(() => {
      if (active) setError('当前用户信息加载失败，请重试。')
    })
    return () => { active = false }
  }, [token, ready, attempt])

  if (!session) {
    return <Navigate to="/login" replace state={{ from: location.pathname + location.search + location.hash }} />
  }
  if (!ready) {
    return (
      <main className="space-y-4 p-6">
        {error ? <>
          <p role="alert" className="text-error">{error}</p>
          <Button onClick={() => setAttempt((value) => value + 1)}>重试</Button>
        </> : <p role="status">正在加载当前用户信息…</p>}
      </main>
    )
  }
  return <Outlet />
}
