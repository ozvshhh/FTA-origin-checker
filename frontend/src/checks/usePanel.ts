import { useCallback } from 'react'
import { useLocation, useSearchParams } from 'react-router-dom'

/** 상세 화면 오른쪽 사이드 패널. 쿼리스트링(?panel=law&code=… / ?panel=pdf&field=…)에 둬서 링크로 공유·새로고침해도 유지된다 */
export type PanelKind = 'law' | 'pdf' | 'mail'
const KINDS: PanelKind[] = ['law', 'pdf', 'mail']

export function usePanel() {
  const [params, setParams] = useSearchParams()
  const raw = params.get('panel')
  const panel = KINDS.includes(raw as PanelKind) ? (raw as PanelKind) : null
  const code = params.get('code')
  const page = Math.max(1, Number(params.get('page')) || 1)
  const field = params.get('field')
  // 같은 항목을 다시 눌러도 주소는 그대로라, 누를 때마다 바뀌는 값을 따로 실어 PDF 강조를 다시 돌린다
  const focusKey = (useLocation().state as { focus?: number } | null)?.focus ?? 0

  const open = useCallback(
    (kind: PanelKind, opts: { code?: string; page?: number; field?: string } = {}) =>
      setParams((prev) => {
        const p = new URLSearchParams(prev)
        p.set('panel', kind)
        for (const k of ['code', 'page', 'field']) p.delete(k)
        if (opts.code) p.set('code', opts.code)
        if (opts.page) p.set('page', String(opts.page))
        if (opts.field) p.set('field', opts.field)
        return p
      }, { replace: true, state: { focus: Date.now() } }),
    [setParams],
  )

  const close = useCallback(
    () =>
      setParams((prev) => {
        const p = new URLSearchParams(prev)
        for (const k of ['panel', 'code', 'page', 'field']) p.delete(k)
        return p
      }, { replace: true }),
    [setParams],
  )

  /** 같은 패널 버튼을 다시 누르면 닫힌다 */
  const toggle = useCallback((kind: PanelKind) => (panel === kind ? close() : open(kind)), [panel, open, close])

  return { panel, code, page, field, focusKey, open, close, toggle }
}
