import { Link, Outlet, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/useAuth'

/** 로그인 후 공통 레이아웃: 상단 GNB(로고 · 사용자 이름 · 로그아웃) */
export function Layout() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()
  return (
    <>
      <header className="gnb">
        <Link to="/checks" className="logo">
          FTA 원산지 서류 점검
        </Link>
        <div className="gnb-right">
          <span>{user?.name}</span>
          <button
            type="button"
            onClick={() => {
              logout()
              navigate('/')
            }}
          >
            로그아웃
          </button>
        </div>
      </header>
      <main className="container">
        <Outlet />
      </main>
    </>
  )
}
