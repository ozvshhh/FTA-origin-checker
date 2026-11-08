import type { CheckStatus, Decision } from '../api/types'
import { DECISION_TONE, STATUS_TONE } from '../styles/tokens'

const STATUS_LABEL: Record<CheckStatus, string> = {
  queued: '대기',
  in_progress: '처리 중',
  completed: '완료',
  failed: '실패',
}

/** 완료 건은 판정을, 그 외는 처리 상태를 보여 준다. 색 규칙은 화면명세서 §3.3 (styles/tokens.ts) */
export function StatusBadge({ status, decision }: { status: CheckStatus; decision?: Decision | null }) {
  const showDecision = status === 'completed' && decision
  const label = showDecision ? decision : STATUS_LABEL[status]
  const tone = showDecision ? DECISION_TONE[decision] : STATUS_TONE[status]
  const busy = status === 'queued' || status === 'in_progress'
  return <span className={`badge tone-${tone}${busy ? ' is-busy' : ''}`}>{label}</span>
}
