import { useCallback, useEffect, useMemo, useState, type ReactNode } from 'react'
import { authApi } from '../api/auth'
import { setUnauthorizedHandler, tokenStore } from '../api/client'
import type { LoginRequest, MeResponse, SignupRequest } from '../api/types'
import { AuthContext } from './context'
import { DEV_LOGIN, DEV_USER } from './devLogin'

export function AuthProvider({ children }: { children: ReactNode }) {
  // 개발용 로그인 모드(devLogin.ts): 처음부터 로그인된 상태, 로그아웃·401 로도 풀리지 않는다
  const [user, setUser] = useState<MeResponse | null>(DEV_LOGIN ? DEV_USER : null)
  const [loading, setLoading] = useState(() => !DEV_LOGIN && tokenStore.get() != null)

  const logout = useCallback(() => {
    if (DEV_LOGIN) return
    tokenStore.clear()
    setUser(null)
  }, [])

  useEffect(() => {
    if (DEV_LOGIN) return
    setUnauthorizedHandler(() => setUser(null))
    return () => setUnauthorizedHandler(null)
  }, [])

  useEffect(() => {
    if (DEV_LOGIN || !tokenStore.get()) return
    authApi
      .me()
      .then(setUser)
      .catch(() => logout())
      .finally(() => setLoading(false))
  }, [logout])

  const login = useCallback(async (req: LoginRequest) => {
    if (DEV_LOGIN) return setUser(DEV_USER)
    const { access_token } = await authApi.login(req)
    tokenStore.set(access_token)
    setUser(await authApi.me())
  }, [])

  const signup = useCallback(async (req: SignupRequest) => {
    if (DEV_LOGIN) return setUser(DEV_USER)
    const { access_token } = await authApi.signup(req) // 가입 성공 → 자동 로그인
    tokenStore.set(access_token)
    setUser(await authApi.me())
  }, [])

  const value = useMemo(() => ({ user, loading, login, signup, logout }), [user, loading, login, signup, logout])
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
