import { apiFetch, apiRaw } from './client'
import type {
  CheckStatus,
  OriginCheckCreatedResponse,
  OriginCheckDetail,
  OriginCheckSummary,
  PageResponse,
} from './types'

export const originChecksApi = {
  /** 서류 묶음 PDF 접수. 결과는 get() 으로 조회한다(처리에 수 분 걸림). */
  /**
   * 한 선적 건의 PDF 들을 접수. 여러 개면 서버가 올린 순서대로 이어 붙여 점검한다.
   * hints = 파일과 같은 순서의 서류 종류 짐작(파일명 기준, 없으면 빈 문자열). 판정에는 쓰이지 않는다.
   */
  create: (files: File[], title?: string, hints?: (string | null)[]) => {
    const form = new FormData()
    files.forEach((f, i) => {
      form.append('file', f)
      form.append('hint', hints?.[i] ?? '')
    })
    if (title) form.append('title', title)
    return apiFetch<OriginCheckCreatedResponse>('/api/v1/origin-checks', { method: 'POST', body: form })
  },

  list: (params: { page?: number; size?: number; status?: CheckStatus } = {}) => {
    const q = new URLSearchParams()
    if (params.page != null) q.set('page', String(params.page))
    if (params.size != null) q.set('size', String(params.size))
    if (params.status) q.set('status', params.status)
    const qs = q.toString()
    return apiFetch<PageResponse<OriginCheckSummary>>(`/api/v1/origin-checks${qs ? `?${qs}` : ''}`)
  },

  /** 담당자 완료 처리 / 되돌리기. 처리 중인 건은 409 CHECK_NOT_FINISHED */
  markDone: (id: number) => apiFetch<OriginCheckSummary>(`/api/v1/origin-checks/${id}/done`, { method: 'PUT' }),
  reopen: (id: number) => apiFetch<OriginCheckSummary>(`/api/v1/origin-checks/${id}/done`, { method: 'DELETE' }),

  /** 실패한 건 다시 실행(같은 파일). 202 {id, status: 'queued'} → 다시 get() 폴링. 실패가 아니면 409 CHECK_NOT_FAILED */
  retry: (id: number) => apiFetch<OriginCheckCreatedResponse>(`/api/v1/origin-checks/${id}/retry`, { method: 'POST' }),

  /** 삭제(원본 파일 포함, 되돌릴 수 없음). 204. 처리 중이면 409 CHECK_IN_PROGRESS */
  remove: (id: number) => apiFetch<void>(`/api/v1/origin-checks/${id}`, { method: 'DELETE' }),

  get: (id: number | string) => apiFetch<OriginCheckDetail>(`/api/v1/origin-checks/${id}`),

  /** 원본 PDF. 인증 헤더가 필요해서 Blob 으로 받아 object URL 로 넘긴다. */
  fileBlob: async (id: number | string) => (await apiRaw(`/api/v1/origin-checks/${id}/file`)).blob(),
}
