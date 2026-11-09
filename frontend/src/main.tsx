import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { BrowserRouter } from 'react-router-dom'
import App from './App'
import { AuthProvider } from './auth/AuthContext'
import { tokenStore } from './api/client'
import { DEV_LOGIN, DEV_TOKEN } from './auth/devLogin'
import './index.css'

// 개발용 로그인 모드: API 요청에 붙일 토큰도 미리 넣어 둔다 (가짜 API 는 아무 토큰이나 받는다)
if (DEV_LOGIN && !tokenStore.get()) {
  tokenStore.set(DEV_TOKEN)
}

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <BrowserRouter>
      <AuthProvider>
        <App />
      </AuthProvider>
    </BrowserRouter>
  </StrictMode>,
)
