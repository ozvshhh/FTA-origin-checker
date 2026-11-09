import type { OriginCheckSummary } from '../api/types'

/**
 * 목록 한 줄 요약: "뭐가 문제인지 → 뭘 해야 하는지".
 * 실무자가 쓰는 짧은 말로 쓴다 (C/O = 원산지증명서, 확인서 = 원산지(포괄)확인서).
 */
const ISSUE: Record<string, { what: string; todo?: string }> = {
  SUP_CRITERION_NOT_MET: { what: '확인서 결정기준 미충족', todo: '특혜 불가' },
  SUP_ORIGIN_MISMATCH: { what: '확인서 원산지 한국 아님', todo: '특혜 불가' },
  CO_INVOICE_NO_MISMATCH: { what: 'C/O 송장번호 불일치', todo: '재발급 요청' },
  CO_QTY_MISMATCH: { what: 'C/O 수량 불일치', todo: '정정 요청' },
  CO_CRITERION_MISMATCH: { what: 'C/O 결정기준 불일치', todo: '정정 요청' },
  CO_ORIGIN_MISMATCH: { what: 'C/O 생산국 불일치', todo: '정정 요청' },
  CO_CONSIGNEE_MISMATCH: { what: 'C/O 수하인 다름', todo: '정정 요청' },
  CO_DESCRIPTION_MISMATCH: { what: 'C/O 품명 다름', todo: '정정 요청' },
  CO_HS_MISMATCH: { what: 'HS 6단위 불일치', todo: '품목분류 확인' },
  SUP_EXPIRED: { what: '확인서 기간 만료', todo: '공급사에 재요청' },
  SUP_RECIPIENT_MISMATCH: { what: '확인서 공급받는 자 다름', todo: '공급사에 재요청' },
  DUPLICATE_DOCUMENT: { what: '같은 서류 2건', todo: '사용할 서류 선택' },
  EXPDEC_INVOICE_NO_MISMATCH: { what: '신고필증 송품장부호 다름' },
  EXPDEC_QTY_MISMATCH: { what: '신고필증 수량 다름' },
  CO_HS_MINOR_DIFF: { what: 'HS 10단위 끝자리 차이' },
  CO_TRANSPORT_MISMATCH: { what: '운송정보 ≠ B/L', todo: '직접운송 확인' },
  CO_ISSUE_DATE_LATE: { what: 'C/O 발급일 지연', todo: '소급발급 확인' },
  SUP_AGREEMENT_MISMATCH: { what: '확인서 협정명 다름' },
  CO_CRITERION_NOT_PSR: { what: '결정기준이 PSR에 없음' },
  QTY_MISMATCH: { what: '패킹리스트 수량 다름' },
  NET_GT_GROSS: { what: '순중량 > 총중량' },
}

const MISSING_TODO: Record<string, string> = {
  인보이스: '영업팀에 요청',
  원산지증명서: '발급 신청 확인',
  원산지확인서: '공급사에 요청',
  'B/L': '선사에 요청',
  수출신고필증: '수리 후 첨부',
}

export interface IssueLine {
  text: string
  todo?: string
  /** 같이 걸린 나머지 항목 수 */
  more: number
}

/** 목록 행의 셋째 줄. 판정 전·실패도 여기서 문구를 정한다 */
export function issueLine(c: OriginCheckSummary): IssueLine | null {
  if (c.status === 'queued') return { text: '점검 대기', more: 0 }
  if (c.status === 'in_progress') return { text: '점검 중', more: 0 }
  if (c.status === 'failed') return { text: '점검 실패', todo: '다시 올리기', more: 0 }
  const codes = c.mismatch_codes
  const missing = c.missing_documents
  if (codes == null && missing == null) return null // 요약 필드가 없는 응답(현재 실제 API)
  const items: IssueLine[] = [
    ...(missing ?? []).map((d) => ({ text: `${d} 누락`, todo: MISSING_TODO[d], more: 0 })),
    ...(codes ?? []).map((code) => ({ text: ISSUE[code]?.what ?? code, todo: ISSUE[code]?.todo, more: 0 })),
  ]
  if (items.length === 0) return { text: '문제 없음', more: 0 }
  // 특혜 불가(1단계) → 할 일 있는 것 → 나머지 순으로 대표 하나
  const first = items.find((x) => x.todo === '특혜 불가') ?? items.find((x) => x.todo) ?? items[0]
  return { ...first, more: items.length - 1 }
}
