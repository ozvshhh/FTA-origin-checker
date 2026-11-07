import { createContext } from 'react'
import type { LoginRequest, MeResponse, SignupRequest } from '../api/types'

export interface AuthState {
  user: MeResponse | null
  /** 저장된 토큰으로 내 정보를 확인하는 중 */
  loading: boolean
  login: (req: LoginRequest) => Promise<void>
  signup: (req: SignupRequest) => Promise<void>
  logout: () => void
}

export const AuthContext = createContext<AuthState | null>(null)
