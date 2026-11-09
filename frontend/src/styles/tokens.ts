// CSS 토큰(tokens.css)과 짝을 이루는 JS 쪽 상수. 판정·상태·신뢰도 → 톤 매핑을 한 곳에서 관리한다.
import type { CheckStatus, Decision } from '../api/types'

/** tokens.css 의 --tone-{name}-* 와 1:1 */
export type Tone = 'success' | 'warning' | 'danger' | 'caution' | 'info' | 'neutral'

/** 화면명세서 §3.3 상태 배지 색 규칙 */
export const DECISION_TONE: Record<Decision, Tone> = {
  '특혜 적용': 'success',
  '보완 요청': 'warning',
  미적용: 'danger',
  '검토 필요': 'caution',
}

export const STATUS_TONE: Record<CheckStatus, Tone> = {
  queued: 'info',
  in_progress: 'info',
  completed: 'neutral', // 완료 건은 보통 DECISION_TONE 을 쓴다
  failed: 'neutral',
}

/** 화면명세서 §5.3.3 신뢰도 임계값. 백엔드가 기준을 주면 그 값으로 덮어쓴다. */
export const CONFIDENCE_THRESHOLD = { low: 0.7, high: 0.9 } as const

/** tokens.css 의 --hl-{level}-* 와 1:1. 우선순위: mismatch > low > mid > high, selected 는 덮어씀 */
export type HighlightLevel = 'mismatch' | 'low' | 'mid' | 'high' | 'selected'

export function confidenceLevel(confidence: number): Exclude<HighlightLevel, 'mismatch' | 'selected'> {
  if (confidence < CONFIDENCE_THRESHOLD.low) return 'low'
  if (confidence < CONFIDENCE_THRESHOLD.high) return 'mid'
  return 'high'
}
