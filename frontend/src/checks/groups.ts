import type { Decision, OriginCheckSummary } from '../api/types'

/** 목록 탭·묶음·필터 정의 (ChecksContext 와 화면들이 같이 쓴다) */
export type TabKey = 'all' | Decision | 'busy' | 'failed'
export const TABS: { key: TabKey; label: string }[] = [
  { key: 'all', label: '전체' },
  { key: '보완 요청', label: '보완 요청' },
  { key: '검토 필요', label: '검토 필요' },
  { key: '미적용', label: '미적용' },
  { key: '특혜 적용', label: '특혜 적용' },
  { key: 'busy', label: '처리 중' },
  { key: 'failed', label: '실패' },
]

export const isBusy = (c: { status: string }) => c.status === 'queued' || c.status === 'in_progress'

/** 왼쪽 목록 묶음: 판정 이름이 아니라 '내가 할 일' 기준. 위에서부터 급한 순서 */
/** m-YYYY-MM = 완료 보기의 선적 월 묶음 */
export type GroupKey = 'action' | 'review' | 'busy' | 'done' | `m-${string}`

/** 완료 보기 결과 필터: 전체 · 특혜 적용 · 특혜 미적용(그 밖의 판정) */
export type ResultFilter = 'all' | 'ok' | 'no'
export const RESULT_FILTERS: { key: ResultFilter; label: string }[] = [
  { key: 'all', label: '전체' },
  { key: 'ok', label: '특혜 적용' },
  { key: 'no', label: '특혜 미적용' },
]
export const matchesResult = (c: OriginCheckSummary, r: ResultFilter) =>
  r === 'all' || (r === 'ok') === (c.decision === '특혜 적용')

/** 완료 건을 묶는 달: 선적일 → 없으면 완료 처리한 날 → 등록일 */
export const monthOf = (c: OriginCheckSummary) => (c.shipped_on ?? c.done_at ?? c.created_at).slice(0, 7)
export const dayOf = (c: OriginCheckSummary) => c.shipped_on ?? c.done_at ?? c.created_at
export const monthLabel = (ym: string) => `${ym.slice(0, 4)}년 ${Number(ym.slice(5, 7))}월`
export const GROUPS: { key: GroupKey; label: string; tone: string; match: (c: OriginCheckSummary) => boolean }[] = [
  { key: 'action', label: '조치 필요', tone: 'danger', match: (c) => c.status === 'failed' || (c.status === 'completed' && (c.decision === '보완 요청' || c.decision === '미적용')) },
  { key: 'review', label: '검토 필요', tone: 'caution', match: (c) => c.status === 'completed' && c.decision === '검토 필요' },
  { key: 'busy', label: '점검 중', tone: 'info', match: isBusy },
  { key: 'done', label: '특혜 적용', tone: 'success', match: (c) => c.status === 'completed' && (c.decision === '특혜 적용' || c.decision == null) },
]
export interface CheckGroup {
  key: GroupKey
  label: string
  tone: string
  items: OriginCheckSummary[]
}

export function matchesTab(c: OriginCheckSummary, tab: TabKey) {
  if (tab === 'all') return true
  if (tab === 'busy') return isBusy(c)
  if (tab === 'failed') return c.status === 'failed'
  return c.status === 'completed' && c.decision === tab
}
