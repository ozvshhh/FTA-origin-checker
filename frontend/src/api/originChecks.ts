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
  create: (file: File, title?: string) => {
    const form = new FormData()
    form.append('file', file)
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

  get: (id: number | string) => apiFetch<OriginCheckDetail>(`/api/v1/origin-checks/${id}`),

  /** 원본 PDF. 인증 헤더가 필요해서 Blob 으로 받아 object URL 로 넘긴다. */
  fileBlob: async (id: number | string) => (await apiRaw(`/api/v1/origin-checks/${id}/file`)).blob(),
}
