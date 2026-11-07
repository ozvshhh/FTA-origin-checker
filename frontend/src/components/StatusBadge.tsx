import type { CheckStatus, Decision } from '../api/types'

const STATUS_LABEL: Record<CheckStatus, string> = {
  queued: '대기',
  in_progress: '처리 중',
  completed: '완료',
  failed: '실패',
}

/** 완료 건은 판정을, 그 외는 처리 상태를 보여 준다. 색 규칙은 화면명세서 §3.3 에 맞춰 채운다. */
export function StatusBadge({ status, decision }: { status: CheckStatus; decision?: Decision | null }) {
  const label = status === 'completed' && decision ? decision : STATUS_LABEL[status]
  return <span className={`badge badge-${status}`}>{label}</span>
}
