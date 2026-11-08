/**
 * 개발용 가짜 API (npm run dev:mock). 백엔드 없이 화면 디자인을 확인할 때 쓴다.
 * client.ts 의 apiRaw 가 VITE_MOCK=1 일 때만 이 모듈을 불러온다 → 운영 빌드에는 들어가지 않는다.
 *
 * - 로그인·가입: 아무 값이나 통과
 * - 심사목록: 판정·상태가 섞인 23건
 * - 상세: 완료 건은 set04 실제 agent 결과(check_result_set04.json)에 목록의 판정을 덮어씀
 * - 원본 PDF: set04.pdf
 * - 새 심사: 대기 → 처리 중(4초) → 완료(12초)로 넘어감 (폴링 확인용)
 * - 완료 처리: PUT/DELETE …/done (처리 중인 건은 409)
 * 데이터는 메모리에만 있어서 새로고침하면 처음 상태로 돌아간다.
 */
import type { CheckFile, CheckStatus, Decision, OriginCheckDetail, OriginCheckSummary } from '../api/types'
import { countPages } from '../utils/pdfjs'
import { CODE_BASIS } from '../data/legalBasis'
import set04 from './check_result_set04.json'
import set04PdfUrl from './set04.pdf?url'

const DECISIONS: Decision[] = ['특혜 적용', '보완 요청', '미적용', '검토 필요']
/** 관리번호·메모. 빈 값이면 파일명으로 저장된다 (백엔드와 같은 규칙) */
const MEMOS = ['PO-2610-118', '', '10월 2차 선적', 'PO-2611-007 재발급분', '']
/** 판정별 대표 사례: [불일치 코드, 누락 서류] */
const CASES: Record<Decision, [string[], string[]][]> = {
  '보완 요청': [[['CO_INVOICE_NO_MISMATCH'], []], [['SUP_EXPIRED'], []], [[], ['원산지확인서']], [['CO_QTY_MISMATCH', 'CO_HS_MINOR_DIFF'], []]],
  미적용: [[['SUP_CRITERION_NOT_MET'], []], [['SUP_ORIGIN_MISMATCH'], []]],
  '검토 필요': [[['CO_HS_MINOR_DIFF'], []], [['CO_TRANSPORT_MISMATCH'], []], [['CO_ISSUE_DATE_LATE'], []], [[], ['B/L']]],
  '특혜 적용': [[[], []]],
}
const BUYERS = [
  { consignee: 'KL HOME SUPPLY SDN. BHD.', destination_country: '말레이시아', agreement: '한-아세안', primary_item: 'FROZEN MACKEREL', item_count: 1 },
  { consignee: 'SAIGON APPAREL JSC', destination_country: '베트남', agreement: '한-아세안', primary_item: "MEN'S COTTON T-SHIRTS", item_count: 3 },
  { consignee: 'SIAM AUTO PARTS CO., LTD.', destination_country: '태국', agreement: '한-아세안', primary_item: 'BRAKE PADS', item_count: 2 },
  { consignee: 'PT NUSANTARA TEKSTIL', destination_country: '인도네시아', agreement: '한-아세안', primary_item: 'KNITTED POLYESTER FABRIC', item_count: 1 },
  { consignee: 'HAMBURG FEINKOST GMBH', destination_country: '독일', agreement: '한-EU', primary_item: 'DRIED LAVER', item_count: 2 },
]
const DELAY_MS = 250 // 로딩 스켈레톤이 보이도록 약간 늦춘다

type Row = OriginCheckSummary & { error?: OriginCheckDetail['error']; files?: CheckFile[] }

/** set04.pdf 의 쪽 순서 그대로 서류를 따로 올린 경우 (짝수 행). 원산지확인서는 파일명으로 짐작 못 한 스캔본 */
const SPLIT_FILES: CheckFile[] = [
  { name: 'FORM_AK_0815.pdf', size: 182_000, page_from: 1, page_to: 1, hint_type: '원산지증명서' },
  { name: '수출신고필증_0412.pdf', size: 204_000, page_from: 2, page_to: 2, hint_type: '수출신고필증' },
  { name: 'CI_SM-2609-0044.pdf', size: 158_000, page_from: 3, page_to: 3, hint_type: '인보이스' },
  { name: 'scan_0412.pdf', size: 311_000, page_from: 4, page_to: 4, hint_type: null },
  { name: 'BL_GOLDEN_BRIDGE.pdf', size: 176_000, page_from: 5, page_to: 5, hint_type: 'B/L' },
  { name: 'PL_SM-2609-0044.pdf', size: 119_000, page_from: 6, page_to: 6, hint_type: '패킹리스트' },
]

const now = Date.now()
const day = (offset: number) => new Date(now - offset * 86_400_000).toISOString().slice(0, 10)
const rows: Row[] = Array.from({ length: 23 }, (_, i) => {
  const status: CheckStatus = i === 0 ? 'in_progress' : i === 1 ? 'queued' : i === 5 ? 'failed' : 'completed'
  const created = new Date(now - (i * 3.7 + 0.2) * 3_600_000).toISOString()
  const decision = status === 'completed' ? DECISIONS[i % 4] : null
  const [codes, missing] = decision ? CASES[decision][Math.floor(i / 4) % CASES[decision].length] : [[], []]
  const buyer = BUYERS[i % BUYERS.length]
  const fileName = `사카밤바스피스_미션2_set${String((i % 13) + 1).padStart(2, '0')}.pdf`
  return {
    id: 23 - i,
    title: MEMOS[i % MEMOS.length] || fileName,
    file_name: fileName,
    status,
    decision,
    invoice_no: status === 'completed' ? `SM-26${String(9 + (i % 3)).padStart(2, '0')}-${String(40 + i).padStart(4, '0')}` : null,
    created_at: created,
    completed_at: status === 'completed' ? new Date(Date.parse(created) + 90_000).toISOString() : null,
    error: status === 'failed' ? { code: 'AGENT_TIMEOUT', message: '제출 후 30분 안에 점검이 끝나지 않았습니다.' } : null,
    mismatch_codes: status === 'completed' ? codes : undefined,
    missing_documents: status === 'completed' ? missing : undefined,
    ...(status === 'completed' ? buyer : {}),
    shipped_on: status === 'completed' ? day(2 + (i % 9)) : null,
    done_at: null,
    files: i % 2 === 0
      ? SPLIT_FILES
      : [{ name: fileName, size: 1_250_349, page_from: 1, page_to: 6, hint_type: null }],
  }
})
let nextId = 24

const json = (body: unknown, status = 200) =>
  new Response(JSON.stringify(body), { status, headers: { 'Content-Type': 'application/json' } })
const notFound = (path: string) =>
  json({ code: 'ORIGIN_CHECK_NOT_FOUND', message: '심사를 찾을 수 없습니다.', path, timestamp: new Date().toISOString() }, 404)

const EMPTY_EXTRACTED = {
  인보이스_추출: [], 패킹리스트_추출: [], BL_추출: [], 원산지증명서_추출: [], 원산지확인서_추출: [], 수출신고필증_추출: [],
}

function toDetail(r: Row): OriginCheckDetail {
  const base = {
    id: r.id, title: r.title, file_name: r.file_name, status: r.status,
    created_at: r.created_at, completed_at: r.completed_at, done_at: r.done_at ?? null, files: r.files ?? [],
  }
  if (r.status !== 'completed') {
    return {
      ...base, job_id: r.status === 'queued' ? null : 'mock-job', judge: null, mismatches: [], follow_up: null,
      extracted: EMPTY_EXTRACTED, extracted_meta: EMPTY_EXTRACTED, error: r.error ?? null,
    } as OriginCheckDetail
  }
  const result = structuredClone(set04) as unknown as OriginCheckDetail
  // 목록의 사례(코드·누락 서류)에 맞춰 판정 부분만 바꾼다. 추출값·좌표는 set04 그대로
  const codes = r.mismatch_codes ?? result.judge?.mismatch_codes ?? []
  const missing = r.missing_documents ?? []
  const labels = [...codes.map((c) => CODE_BASIS[c]?.label ?? c), ...missing.map((d) => `${d} 누락`)]
  const sameAsSample = codes.join() === (result.judge?.mismatch_codes ?? []).join() && missing.length === 0
  return {
    ...result,
    ...base,
    follow_up: r.decision === '특혜 적용' ? null : result.follow_up,
    mismatches: codes.map((c) => ({ code: c, stage: CODE_BASIS[c]?.stage ?? null, label: CODE_BASIS[c]?.label ?? c })),
    judge: result.judge && {
      ...result.judge,
      invoice_no: r.invoice_no,
      decision: r.decision ?? result.judge.decision,
      mismatch_codes: codes,
      missing_documents: missing,
      reason: sameAsSample ? result.judge.reason : labels.length ? `${labels.join(', ')}.` : '서류 사이에 불일치가 없습니다.',
    },
  }
}

export async function mockFetch(path: string, init: RequestInit = {}): Promise<Response> {
  await new Promise((r) => setTimeout(r, DELAY_MS))
  const url = new URL(path, location.origin)
  const p = url.pathname
  const method = (init.method ?? 'GET').toUpperCase()

  if (p === '/api/v1/auth/login' || p === '/api/v1/auth/signup') {
    return json({ access_token: 'mock-token', token_type: 'bearer', expires_in: 86_400 })
  }
  if (p === '/api/v1/auth/me') {
    return json({ id: 1, email: 'tester@example.test', name: '테스트 담당자', company: '사카밤바스피스', expires_at: new Date(now + 86_400_000).toISOString() })
  }

  if (p === '/api/v1/origin-checks' && method === 'POST') {
    const form = init.body as FormData
    const files = form.getAll('file') as File[]
    const hints = form.getAll('hint').map(String)
    let page = 1
    const checkFiles: CheckFile[] = []
    for (const [i, f] of files.entries()) {
      const n = (await countPages(f)) ?? 1
      checkFiles.push({ name: f.name, size: f.size, page_from: page, page_to: page + n - 1, hint_type: hints[i] || null })
      page += n
    }
    const first = files[0]?.name ?? 'upload.pdf'
    const bundleName = files.length > 1 ? `${first.replace(/\.pdf$/i, '')} 외 ${files.length - 1}건.pdf` : first
    const row: Row = {
      id: nextId++, title: String(form.get('title') ?? '') || bundleName, file_name: bundleName,
      status: 'queued', decision: null, invoice_no: null, created_at: new Date().toISOString(), completed_at: null, done_at: null,
      files: checkFiles,
    }
    rows.unshift(row)
    setTimeout(() => { row.status = 'in_progress' }, 4_000)
    setTimeout(() => {
      row.status = 'completed'
      row.decision = '보완 요청'
      row.invoice_no = 'SM-2609-0044'
      row.completed_at = new Date().toISOString()
    }, 12_000)
    return json({ id: row.id, status: row.status }, 201)
  }

  if (p === '/api/v1/origin-checks') {
    const page = Number(url.searchParams.get('page') ?? 0)
    const size = Number(url.searchParams.get('size') ?? 20)
    const status = url.searchParams.get('status')
    const list = rows.filter((r) => !status || r.status === status)
    const items = list.slice(page * size, (page + 1) * size).map(({ error: _e, files: _f, ...s }) => s)
    return json({ items, page, size, total_elements: list.length, total_pages: Math.ceil(list.length / size) })
  }

  const doneMatch = p.match(/^\/api\/v1\/origin-checks\/(\d+)\/done$/)
  if (doneMatch && (method === 'PUT' || method === 'DELETE')) {
    const row = rows.find((r) => r.id === Number(doneMatch[1]))
    if (!row) return notFound(p)
    if (method === 'PUT') {
      if (row.status === 'queued' || row.status === 'in_progress') {
        return json({ code: 'CHECK_NOT_FINISHED', message: '처리 중인 점검 건은 완료 처리할 수 없습니다.', path: p, timestamp: new Date().toISOString() }, 409)
      }
      row.done_at ??= new Date().toISOString()
    } else {
      row.done_at = null
    }
    const { error: _e, files: _f, ...summary } = row
    return json(summary)
  }

  const fileMatch = p.match(/^\/api\/v1\/origin-checks\/(\d+)\/file$/)
  if (fileMatch) {
    if (!rows.some((r) => r.id === Number(fileMatch[1]))) return notFound(p)
    return fetch(set04PdfUrl)
  }

  const detailMatch = p.match(/^\/api\/v1\/origin-checks\/(\d+)$/)
  if (detailMatch) {
    const row = rows.find((r) => r.id === Number(detailMatch[1]))
    return row ? json(toDetail(row)) : notFound(p)
  }

  return json({ code: 'NOT_FOUND', message: `가짜 API에 없는 경로: ${method} ${p}`, path: p, timestamp: new Date().toISOString() }, 404)
}
