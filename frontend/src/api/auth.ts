import { apiFetch } from './client'
import type { LoginRequest, MeResponse, SignupRequest, TokenResponse } from './types'

export const authApi = {
  login: (body: LoginRequest) =>
    apiFetch<TokenResponse>('/api/v1/auth/login', { method: 'POST', body: JSON.stringify(body) }),

  signup: (body: SignupRequest) =>
    apiFetch<TokenResponse>('/api/v1/auth/signup', { method: 'POST', body: JSON.stringify(body) }),

  me: () => apiFetch<MeResponse>('/api/v1/auth/me'),
}
