import { Link } from 'react-router-dom'
import { DEV_LOGIN } from '../auth/devLogin'

/** 개발용 로그인 모드일 때만 보이는 페이지 바로가기. 운영 빌드에는 나오지 않는다 */
export function DevBar() {
  if (!DEV_LOGIN) return null
  return (
    <nav className="devbar" aria-label="개발용 페이지 바로가기">
      <span className="devbar-tag">DEV 로그인</span>
      <Link to="/">메인</Link>
      <Link to="/checks">심사목록</Link>
      <Link to="/checks/21">상세</Link>
      <Link to="/__404">404</Link>
    </nav>
  )
}
