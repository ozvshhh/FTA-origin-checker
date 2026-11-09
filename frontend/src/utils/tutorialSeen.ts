const KEY = 'origin-check.tutorial.v1'

/** 이 사용자가 튜토리얼을 이미 봤는가. [백엔드 추가 후보] 지금은 브라우저에만 기억한다 */
export function tutorialSeen(userId: number | string) {
  try {
    return localStorage.getItem(`${KEY}.${userId}`) === 'done'
  } catch {
    return true // 저장소를 못 쓰면 매번 띄우지 않는다
  }
}
export function markSeen(userId: number | string) {
  try {
    localStorage.setItem(`${KEY}.${userId}`, 'done')
  } catch {
    /* noop */
  }
}
