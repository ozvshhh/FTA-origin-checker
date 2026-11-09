import type { MeResponse } from '../api/types'

/**
 * 개발용 로그인 모드. VITE_DEV_LOGIN=1 이면 처음부터 로그인된 상태로 시작하고,
 * 로그인 상태에서도 랜딩·로그인 창·404 를 그대로 볼 수 있다(랜딩이 심사목록으로 보내지 않음).
 *
 * - 개발 서버(npm run dev / dev:mock)에서만 켜진다. 운영 빌드(npm run build)에서는 값과 상관없이 꺼진다.
 * - 끄기: .env.local 에 VITE_DEV_LOGIN=0, 또는 실행할 때 VITE_DEV_LOGIN=0 npm run dev:mock
 * - 데이터까지 보려면 가짜 API(dev:mock)와 같이 쓴다. 진짜 백엔드에 붙일 때는 끄고 실제로 로그인한다
 *   (가짜 토큰이라 백엔드가 401 을 준다).
 */
export const DEV_LOGIN = import.meta.env.DEV && import.meta.env.VITE_DEV_LOGIN === '1'

export const DEV_USER: MeResponse = {
  id: 0,
  email: 'dev@example.test',
  name: '개발용 담당자',
  company: '사카밤바스피스',
  expires_at: '2099-12-31T00:00:00Z',
}

export const DEV_TOKEN = 'dev-login-token'
