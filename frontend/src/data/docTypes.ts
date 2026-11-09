import type { CheckFile, ExtractNode, OriginCheckDetail } from '../api/types'
import { DOC_DEFS } from './documents'

/** 서류 6종 (판정 규칙 기준 순서). 패킹리스트만 선택 서류 */
export const DOC_TYPES = ['인보이스', '원산지증명서', '원산지확인서', 'B/L', '수출신고필증', '패킹리스트'] as const
export type DocType = (typeof DOC_TYPES)[number]

/** 빠지면 어떤 판정으로 이어지는지 (워크플로 판정 규칙: 2단계 보완 요청 / 3단계 검토 필요) */
export const MISSING_EFFECT: Record<DocType, '보완 요청' | '검토 필요' | null> = {
  인보이스: '보완 요청',
  원산지증명서: '보완 요청',
  원산지확인서: '보완 요청',
  'B/L': '검토 필요',
  수출신고필증: '검토 필요',
  패킹리스트: null,
}

/** 파일명 키워드 → 서류 종류 짐작 (기획서 '여러 파일 분류 방법' 1단계). 틀려도 판정에는 영향 없음 */
const HINTS: [DocType, RegExp][] = [
  ['원산지확인서', /원산지\s*(포괄)?\s*확인|포괄\s*확인/i],
  ['원산지증명서', /certificate[\s_-]*of[\s_-]*origin|form[\s_-]*(a[kie]|e)(?![a-z])|(^|[^a-z])c[\s/_-]?o([^a-z]|$)|원산지\s*증명/i],
  ['수출신고필증', /수출\s*신고|신고\s*필증|export[\s_-]*declaration|(^|[^a-z])ed([^a-z]|$)/i],
  ['B/L', /bill[\s_-]*of[\s_-]*lading|(^|[^a-z])b[\s/_-]?l([^a-z]|$)|선하\s*증권/i],
  ['패킹리스트', /packing|(^|[^a-z])p[\s/_-]?l([^a-z]|$)|패킹/i],
  ['인보이스', /invoice|(^|[^a-z])(ci|inv)([^a-z]|$)|인보이스|송장/i],
]

export function hintFromName(fileName: string): DocType | null {
  const base = fileName.replace(/\.pdf$/i, '')
  return HINTS.find(([, re]) => re.test(base))?.[0] ?? null
}

const NODE_TYPE = Object.fromEntries(DOC_DEFS.map((d) => [d.node, d.name])) as Record<ExtractNode, DocType>

/** AI 결과에서 쪽 → 서류 종류. 추출 항목의 page 를 모은다 (기획서 2단계: 워크플로 Classify 결과) */
export function pageTypes(detail: OriginCheckDetail): Map<number, Set<DocType>> {
  const out = new Map<number, Set<DocType>>()
  const walk = (o: unknown, t: DocType) => {
    if (Array.isArray(o)) o.forEach((v) => walk(v, t))
    else if (o && typeof o === 'object') {
      const page = (o as { page?: unknown }).page
      if (typeof page === 'number' && page > 0) {
        if (!out.has(page)) out.set(page, new Set())
        out.get(page)!.add(t)
      }
      Object.values(o).forEach((v) => walk(v, t))
    }
  }
  for (const node of Object.keys(detail.extracted_meta) as ExtractNode[]) walk(detail.extracted_meta[node], NODE_TYPE[node])
  return out
}

/** 파일 하나가 차지한 쪽들에서 AI 가 찾은 서류 종류 */
export function fileTypes(file: CheckFile, pages: Map<number, Set<DocType>>): DocType[] {
  const s = new Set<DocType>()
  for (let p = file.page_from; p <= file.page_to; p++) pages.get(p)?.forEach((t) => s.add(t))
  return DOC_TYPES.filter((t) => s.has(t))
}
