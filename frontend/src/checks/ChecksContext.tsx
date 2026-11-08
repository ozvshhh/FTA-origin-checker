import { createContext, useCallback, useContext, useEffect, useMemo, useRef, useState, type ReactNode } from 'react'
import { useSearchParams } from 'react-router-dom'
import { ApiError } from '../api/client'
import { originChecksApi } from '../api/originChecks'
import type { Decision, OriginCheckSummary } from '../api/types'

/**
 * 심사목록 상태. 왼쪽 목록과 오른쪽 상세(이전/다음 이동)가 같이 쓴다.
 *
 * 목록 API 는 status 거르기만 지원하고 판정별 거르기·검색·건수 집계가 없다.
 * 그래서 내 심사를 최대 MAX_FETCH 건까지 한 번에 받아 탭·검색·정렬을 브라우저에서 한다.
 * 건수가 많아지면 백엔드에 decision·q 쿼리와 탭별 건수를 추가하고 서버 쪽으로 옮긴다.
 */
const FETCH_SIZE = 100
const MAX_FETCH = 500
const POLL_MS = 10_000 // §3.3 처리 중 건이 있으면 10초마다 갱신

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
const matchesResult = (c: OriginCheckSummary, r: ResultFilter) =>
  r === 'all' || (r === 'ok') === (c.decision === '특혜 적용')

/** 완료 건을 묶는 달: 선적일 → 없으면 완료 처리한 날 → 등록일 */
const monthOf = (c: OriginCheckSummary) => (c.shipped_on ?? c.done_at ?? c.created_at).slice(0, 7)
const dayOf = (c: OriginCheckSummary) => c.shipped_on ?? c.done_at ?? c.created_at
const monthLabel = (ym: string) => `${ym.slice(0, 4)}년 ${Number(ym.slice(5, 7))}월`
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

function matchesTab(c: OriginCheckSummary, tab: TabKey) {
  if (tab === 'all') return true
  if (tab === 'busy') return isBusy(c)
  if (tab === 'failed') return c.status === 'failed'
  return c.status === 'completed' && c.decision === tab
}

async function fetchAllChecks() {
  const items: OriginCheckSummary[] = []
  let page = 0
  let totalPages = 1
  while (page < totalPages && items.length < MAX_FETCH) {
    const res = await originChecksApi.list({ page, size: FETCH_SIZE })
    items.push(...res.items)
    totalPages = res.total_pages
    page += 1
  }
  return { items, truncated: page < totalPages }
}

/** 담당자가 '완료 처리'한 심사(done_at 있음)는 진행 중 목록에서 빠지고 '완료' 보기로 옮겨진다 */
export type ListView = 'open' | 'done'

interface ChecksState {
  items: OriginCheckSummary[] | null
  /** 진행 중(아직 완료 처리 안 함) / 완료 보기 */
  view: ListView
  setView: (v: ListView) => void
  /** 보기별 건수 (검색 전) */
  viewCounts: Record<ListView, number>
  /** 완료 처리 시각. 없으면 진행 중 */
  doneAt: (id: number) => string | null
  setDone: (id: number, done: boolean) => Promise<void>
  /** 완료 처리 실패 같은 짧은 알림 (몇 초 뒤 사라짐) */
  notice: string | null
  /**
   * 사용자가 좌측에서 보기(진행 중/완료)나 검색을 바꾼 직후의 목표 값. 주소가 이 값으로 바뀌면 상세가 새 목록을 따라간다.
   * 처리한 쪽이 consumeIntent() 로 지운다.
   */
  /** 사용자가 바꾼 보기·검색·결과 필터. 주소가 이 값이 된 뒤에 상세가 새 목록을 따라간다 */
  userIntent: { view: ListView; q: string; result?: ResultFilter } | null
  consumeIntent: () => void
  /** 방금 만든 심사를 목록에 바로 넣는다 (서버 목록을 다시 받기 전에 좌측에 보이게) */
  addLocal: (row: OriginCheckSummary) => void
  /** 이 심사가 속한 보기 */
  viewOf: (c: OriginCheckSummary) => ListView
  /** 할 일 묶음 (검색 반영) */
  groups: CheckGroup[]
  /** groups 를 위에서부터 이어 붙인 순서 → 상세의 이전/다음 이동이 목록 순서와 같다 */
  filtered: OriginCheckSummary[]
  counts: Record<TabKey, number>
  error: string | null
  truncated: boolean
  tab: TabKey
  q: string
  sort: 'latest' | 'oldest'
  /** 완료 보기에서만 쓰는 결과 필터 */
  result: ResultFilter
  setFilter: (next: Partial<{ tab: TabKey; q: string; sort: 'latest' | 'oldest'; result: ResultFilter }>) => void
  reload: () => void
}

const Ctx = createContext<ChecksState | null>(null)

export function ChecksProvider({ children }: { children: ReactNode }) {
  const [params, setParams] = useSearchParams()
  const tab = (TABS.find((t) => t.key === params.get('tab'))?.key ?? 'all') as TabKey
  const q = params.get('q') ?? ''
  const sort = params.get('sort') === 'oldest' ? 'oldest' : 'latest'
  const view: ListView = params.get('view') === 'done' ? 'done' : 'open'
  const result = (RESULT_FILTERS.find((r) => r.key === params.get('result'))?.key ?? 'all') as ResultFilter
  const [notice, setNotice] = useState<string | null>(null)
  const [userIntent, setUserIntent] = useState<{ view: ListView; q: string; result?: ResultFilter } | null>(null)
  const consumeIntent = useCallback(() => setUserIntent(null), [])
  const cur = useRef({ view, q })
  useEffect(() => {
    cur.current = { view, q }
  }, [view, q])

  const [items, setItems] = useState<OriginCheckSummary[] | null>(null)
  const itemsRef = useRef(items)
  useEffect(() => {
    itemsRef.current = items
  }, [items])
  const [truncated, setTruncated] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [reloadKey, setReloadKey] = useState(0)

  useEffect(() => {
    let cancelled = false
    let timer: ReturnType<typeof setTimeout> | undefined
    const load = async () => {
      try {
        const res = await fetchAllChecks()
        if (cancelled) return
        setItems(res.items)
        setTruncated(res.truncated)
        setError(null)
        if (res.items.some(isBusy)) timer = setTimeout(load, POLL_MS)
      } catch (e) {
        if (cancelled) return
        setError(e instanceof ApiError ? e.message : '목록을 불러오지 못했습니다.')
      }
    }
    load()
    return () => {
      cancelled = true
      clearTimeout(timer)
    }
  }, [reloadKey])

  /** 필터는 쿼리스트링에 둔다 → 상세로 이동해도, 새로고침해도 유지된다 */
  const setFilter = useCallback<ChecksState['setFilter']>(
    (next) => {
      if (next.q != null) setUserIntent({ view: cur.current.view, q: next.q })
      // 결과 필터를 바꿔 지금 상세가 목록에서 빠지면 새 목록 첫 건으로 (보기·검색은 그대로)
      else if (next.result != null) setUserIntent({ view: cur.current.view, q: cur.current.q, result: next.result })
      setParams(
        (prev) => {
          const p = new URLSearchParams(prev)
          const put = (k: string, v: string | undefined, dflt: string) => {
            if (v == null) return
            if (v === dflt || v === '') p.delete(k)
            else p.set(k, v)
          }
          put('tab', next.tab, 'all')
          put('q', next.q, '')
          put('sort', next.sort, 'latest')
          put('result', next.result, 'all')
          return p
        },
        { replace: true },
      )
    },
    [setParams],
  )

  const reload = useCallback(() => setReloadKey((k) => k + 1), [])

  const setView = useCallback(
    (v: ListView) => {
      setUserIntent({ view: v, q: cur.current.q })
      setParams((prev) => {
        const p = new URLSearchParams(prev)
        if (v === 'open') p.delete('view')
        else p.set('view', v)
        return p
      }, { replace: true })
    },
    [setParams],
  )
  const addLocal = useCallback((row: OriginCheckSummary) => {
    setItems((prev) => [row, ...(prev ?? []).filter((c) => c.id !== row.id)])
  }, [])
  // 화면부터 바로 바꾸고(낙관적 갱신) 서버에 저장한다. 실패하면 되돌리고 알림
  const setDone = useCallback(async (id: number, isDone: boolean) => {
    const patch = (doneAtValue: string | null) =>
      setItems((prev) => prev && prev.map((c) => (c.id === id ? { ...c, done_at: doneAtValue } : c)))
    const before = itemsRef.current?.find((c) => c.id === id)?.done_at ?? null
    patch(isDone ? new Date().toISOString() : null)
    try {
      const saved = isDone ? await originChecksApi.markDone(id) : await originChecksApi.reopen(id)
      patch(saved.done_at ?? null)
    } catch (e) {
      patch(before)
      setNotice(e instanceof ApiError ? e.message : '저장하지 못했습니다. 다시 시도하세요.')
      setTimeout(() => setNotice(null), 4000)
    }
  }, [])
  const doneAt = useCallback((id: number) => items?.find((c) => c.id === id)?.done_at ?? null, [items])
  const isDoneItem = useCallback((c: OriginCheckSummary) => !isBusy(c) && c.done_at != null, [])
  const viewOf = useCallback((c: OriginCheckSummary): ListView => (isDoneItem(c) ? 'done' : 'open'), [isDoneItem])
  const viewCounts = useMemo(() => {
    const all = items ?? []
    const d = all.filter(isDoneItem).length
    return { open: all.length - d, done: d }
  }, [items, isDoneItem])

  const counts = useMemo(() => {
    const c = Object.fromEntries(TABS.map((t) => [t.key, 0])) as Record<TabKey, number>
    for (const it of items ?? []) if (!isDoneItem(it)) for (const t of TABS) if (matchesTab(it, t.key)) c[t.key] += 1
    return c
  }, [items, isDoneItem])

  const groups = useMemo(() => {
    const needle = q.trim().toLowerCase()
    const rows = (items ?? []).filter(
      (c) =>
        isDoneItem(c) === (view === 'done') &&
        matchesTab(c, tab) &&
        (!needle || [c.title, c.invoice_no, c.file_name, c.consignee, c.primary_item, c.destination_country].some((v) => v?.toLowerCase().includes(needle))),
    )
    if (view === 'done') {
      // 완료 보기: 선적 월별 묶음(최근 달부터), 달 안에서는 선적일 최근 순. 결과 필터 반영
      const byMonth = new Map<string, OriginCheckSummary[]>()
      for (const c of rows.filter((c) => matchesResult(c, result)).sort((a, b) => dayOf(b).localeCompare(dayOf(a)))) {
        const m = monthOf(c)
        byMonth.set(m, [...(byMonth.get(m) ?? []), c])
      }
      return [...byMonth.entries()]
        .sort(([a], [b]) => b.localeCompare(a))
        .map(([m, list]): CheckGroup => ({ key: `m-${m}`, label: monthLabel(m), tone: 'neutral', items: list }))
    }
    rows.sort((a, b) => (sort === 'latest' ? -1 : 1) * a.created_at.localeCompare(b.created_at))
    return GROUPS.map(({ key, label, tone, match }) => ({ key, label, tone, items: rows.filter(match) }))
  }, [items, tab, q, sort, view, result, isDoneItem])
  const filtered = useMemo(() => groups.flatMap((g) => g.items), [groups])

  const value = useMemo(
    () => ({ items, view, setView, viewCounts, doneAt, setDone, notice, userIntent, consumeIntent, addLocal, viewOf, groups, filtered, counts, error, truncated, tab, q, sort, result, setFilter, reload }),
    [items, view, setView, viewCounts, doneAt, setDone, notice, userIntent, consumeIntent, addLocal, viewOf, groups, filtered, counts, error, truncated, tab, q, sort, result, setFilter, reload],
  ) satisfies ChecksState

  return <Ctx.Provider value={value}>{children}</Ctx.Provider>
}

export function useChecks() {
  const v = useContext(Ctx)
  if (!v) throw new Error('useChecks 는 ChecksProvider 안에서만 쓸 수 있습니다.')
  return v
}
