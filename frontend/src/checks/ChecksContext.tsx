import { useCallback, useEffect, useMemo, useRef, useState, type ReactNode } from 'react'
import { useSearchParams } from 'react-router-dom'
import { ApiError } from '../api/client'
import { originChecksApi } from '../api/originChecks'
import type { OriginCheckSummary } from '../api/types'
import { dayOf, GROUPS, RESULT_FILTERS, isBusy, matchesResult, matchesTab, monthLabel, monthOf, TABS, type CheckGroup, type ResultFilter, type TabKey } from './groups'
import { ChecksCtx, type ChecksState, type ListView } from './useChecks'

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
  const patchLocal = useCallback((id: number, patch: Partial<OriginCheckSummary>) => {
    setItems((prev) => prev && prev.map((c) => (c.id === id ? { ...c, ...patch } : c)))
  }, [])
  const removeLocal = useCallback((id: number) => {
    setItems((prev) => prev && prev.filter((c) => c.id !== id))
  }, [])
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
    () => ({ items, view, setView, viewCounts, doneAt, setDone, notice, userIntent, consumeIntent, addLocal, patchLocal, removeLocal, viewOf, groups, filtered, counts, error, truncated, tab, q, sort, result, setFilter, reload }),
    [items, view, setView, viewCounts, doneAt, setDone, notice, userIntent, consumeIntent, addLocal, patchLocal, removeLocal, viewOf, groups, filtered, counts, error, truncated, tab, q, sort, result, setFilter, reload],
  ) satisfies ChecksState

  return <ChecksCtx.Provider value={value}>{children}</ChecksCtx.Provider>
}

