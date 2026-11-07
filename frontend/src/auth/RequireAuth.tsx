import { Navigate, Outlet, useLocation } from 'react-router-dom'
import { useAuth } from './useAuth'

/** 로그인해야 볼 수 있는 화면. 아니면 메인(S-01)으로 보낸다. */
export function RequireAuth() {
  const { user, loading } = useAuth()
  const location = useLocation()
  if (loading) return <p className="muted">불러오는 중…</p>
  if (!user) return <Navigate to="/" replace state={{ from: location.pathname }} />
  return <Outlet />
}
