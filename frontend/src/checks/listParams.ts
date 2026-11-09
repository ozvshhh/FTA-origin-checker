/** 상세에서 맨 위 심사일 때 ↑ 를 누르면 보내는 이벤트 이름 */
export const COLLAPSE_ACTIVE_GROUP = 'checks:collapse-active-group'

/** 목록 필터(tab·q·sort)만 남긴 쿼리스트링. 상세 패널 상태(panel 등)는 목록 링크에 싣지 않는다 */
export function keepListParams(search: string) {
  const src = new URLSearchParams(search)
  const out = new URLSearchParams()
  for (const k of ['tab', 'q', 'sort', 'view', 'result']) {
    const v = src.get(k)
    if (v) out.set(k, v)
  }
  const s = out.toString()
  return s ? `?${s}` : ''
}
