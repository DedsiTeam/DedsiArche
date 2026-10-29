import { Navigate, Outlet, useLocation } from 'react-router-dom'
import { useSession } from '@/services/auth'

export function RequireAuth() {
  const session = useSession()
  const location = useLocation()

  if (!session) {
    return <Navigate to="/login" replace state={{ from: location.pathname + location.search + location.hash }} />
  }
  return <Outlet />
}
