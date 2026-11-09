import { Fragment, useCallback, useEffect, useLayoutEffect, useRef, useState, type RefObject } from 'react'
import type { PDFDocumentLoadingTask, PDFDocumentProxy, PDFPageProxy, RenderTask } from 'pdfjs-dist'
import { openPdf } from '../utils/pdfjs'
import type { Highlight } from '../data/highlights'
import { IconMinus, IconPlus } from './icons'


/**
 * 보기 전용 PDF 뷰어 (pdf.js 로 canvas 렌더링).
 * - 위아래 스크롤로 모든 쪽을 이어서 본다. 화면 근처 쪽만 그린다.
 * - 확대·축소: 아래 작은 버튼, 또는 Ctrl/⌘ + 휠(트랙패드 핀치). 기본은 폭 맞춤.
 * - highlights: 페이지 대비 0~1 좌표 박스. focusId 가 바뀌면 그 박스로 스크롤하고 원래 색이 잠깐 진해졌다 돌아온다.
 */
export interface PdfViewerProps {
  data: ArrayBuffer
  highlights?: Highlight[]
  /** 이 박스를 선택 상태로 강조하고 스크롤 */
  focusId?: string | null
  /** 이 코드에 걸린 박스들을 강조하고 첫 박스로 스크롤 */
  focusCode?: string | null
  /** 같은 대상을 다시 골랐을 때도 강조를 다시 돌리기 위한 값 (누를 때마다 바뀐다) */
  focusKey?: number
  /** focusId·focusCode 가 없을 때 처음 보여 줄 쪽 */
  initialPage?: number
  /** true 면 신뢰도 높은 박스(초록)도 보인다 */
  showAll?: boolean
}

const ZOOM_MIN = 0.5
const ZOOM_MAX = 3
const ZOOM_STEP = 1.2
const GAP = 12
const PAD = 12

type PageSize = { w: number; h: number }

export function PdfViewer({ data, highlights = [], focusId, focusCode, focusKey = 0, initialPage = 1, showAll = false }: PdfViewerProps) {
  const rootRef = useRef<HTMLDivElement>(null)
  const scrollRef = useRef<HTMLDivElement>(null)
  const [hover, setHover] = useState<{ hl: Highlight; box: DOMRect } | null>(null)
  const [doc, setDoc] = useState<PDFDocumentProxy | null>(null)
  const [sizes, setSizes] = useState<PageSize[]>([])
  const [error, setError] = useState<string | null>(null)
  const [width, setWidth] = useState(0)
  const [zoom, setZoom] = useState(1)
  const [current, setCurrent] = useState(1)

  // 문서 열기 (pdf.js 가 버퍼를 넘겨받아 비우므로 복사본을 넘긴다)
  useEffect(() => {
    let cancelled = false
    let task: PDFDocumentLoadingTask | null = null
    openPdf(new Uint8Array(data.slice(0)))
      .then((t) => {
        task = t
        if (cancelled) t.destroy()
        return t.promise
      })
      .then(async (d) => {
        const s: PageSize[] = []
        for (let i = 1; i <= d.numPages; i++) {
          const vp = (await d.getPage(i)).getViewport({ scale: 1 })
          s.push({ w: vp.width, h: vp.height })
        }
        if (cancelled) return
        setDoc(d)
        setSizes(s)
      })
      .catch(() => !cancelled && setError('PDF 를 열지 못했습니다.'))
    return () => {
      cancelled = true
      task?.destroy()
    }
  }, [data])

  // 컨테이너 폭 → 폭 맞춤 배율
  useEffect(() => {
    const el = scrollRef.current
    if (!el) return
    setWidth(el.clientWidth) // 첫 폭은 바로 읽는다 (ResizeObserver 첫 알림이 늦게 오는 환경 대비)
    const ro = new ResizeObserver(([e]) => setWidth(e.contentRect.width))
    ro.observe(el)
    return () => ro.disconnect()
  }, [])
  const maxW = Math.max(1, ...sizes.map((s) => s.w))
  const fit = width > 0 ? (width - PAD * 2) / maxW : 1
  const scale = fit * zoom

  // 확대·축소해도 보던 위치를 유지
  const anchor = useRef<number | null>(null)
  const changeZoom = useCallback((next: (z: number) => number) => {
    const el = scrollRef.current
    if (el) anchor.current = (el.scrollTop + el.clientHeight / 2) / Math.max(1, el.scrollHeight)
    setZoom((z) => Math.min(ZOOM_MAX, Math.max(ZOOM_MIN, next(z))))
  }, [])
  useLayoutEffect(() => {
    const el = scrollRef.current
    if (!el || anchor.current == null) return
    el.scrollTop = anchor.current * el.scrollHeight - el.clientHeight / 2
    anchor.current = null
  }, [zoom])

  // Ctrl/⌘ + 휠 (트랙패드 핀치) 확대
  useEffect(() => {
    const el = scrollRef.current
    if (!el) return
    const onWheel = (e: WheelEvent) => {
      if (!e.ctrlKey && !e.metaKey) return
      e.preventDefault()
      changeZoom((z) => z * Math.exp(-e.deltaY * 0.01))
    }
    el.addEventListener('wheel', onWheel, { passive: false })
    return () => el.removeEventListener('wheel', onWheel)
  }, [changeZoom])

  // 쪽 위치 (스크롤·하이라이트 이동 계산용)
  const pageTop = useCallback(
    (n: number) => PAD + sizes.slice(0, n - 1).reduce((acc, s) => acc + s.h * scale + GAP, 0),
    [sizes, scale],
  )

  // 지금 보고 있는 쪽
  const onScroll = () => {
    setHover(null)
    const el = scrollRef.current
    if (!el || sizes.length === 0) return
    const mid = el.scrollTop + el.clientHeight / 3
    let n = 1
    for (let i = 1; i <= sizes.length; i++) if (pageTop(i) <= mid) n = i
    setCurrent(n)
  }

  // 포커스 이동: 박스 → 코드 → 쪽
  const selectedIds = new Set<string>()
  if (focusId) selectedIds.add(focusId)
  if (focusCode) for (const h of highlights) if (h.codes.includes(focusCode)) selectedIds.add(h.id)
  const target = highlights.find((h) => h.id === focusId) ?? (focusCode ? highlights.find((h) => h.codes.includes(focusCode)) : undefined)
  // 깜빡임은 스크롤이 멈춘 뒤에 시작한다 (0 = 아직 깜빡이지 않음). 이동 중에 끝나 버리면 눈에 안 띈다
  const [flashKey, setFlashKey] = useState(0)
  useEffect(() => {
    const el = scrollRef.current
    if (!el || sizes.length === 0 || width === 0) return
    if (target) {
      const p = sizes[target.page - 1]
      if (!p) return
      const y = pageTop(target.page) + target.rect.y * p.h * scale
      const reduce = window.matchMedia('(prefers-reduced-motion: reduce)').matches
      el.scrollTo({ top: Math.max(0, y - el.clientHeight / 3), behavior: reduce ? 'auto' : 'smooth' })
      setFlashKey(0)
      // scrollTop 이 150ms 동안 그대로면 도착한 것으로 본다 (부드러운 스크롤 끝 + 쪽 그림이 그려질 틈)
      let last = -1
      let still = 0
      const started = Date.now()
      const timer = window.setInterval(() => {
        const top = el.scrollTop
        still = top === last ? still + 1 : 0
        last = top
        if (still >= 3 || Date.now() - started > 2000) {
          window.clearInterval(timer)
          setFlashKey((k) => k + 1)
        }
      }, 50)
      return () => window.clearInterval(timer)
    } else if (initialPage > 1) {
      el.scrollTo({ top: pageTop(initialPage) - PAD })
    }
    // 포커스 대상이 바뀔 때만 이동한다 (확대·축소 때는 위치 유지 로직이 맡는다)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [target?.id, focusCode, focusKey, initialPage, sizes.length, width > 0])

  if (error) return <div className="pv-state">{error}</div>

  return (
    <div className="pv" ref={rootRef}>
      <div className="pv-scroll" ref={scrollRef} onScroll={onScroll}>
        {!doc && <div className="pv-state"><span className="status-ring tone-info is-busy" /></div>}
        {doc && width > 0 && (
          <div className="pv-pages" style={{ padding: PAD, gap: GAP }}>
            {sizes.map((s, i) => (
              <PageView
                key={i}
                doc={doc}
                pageNumber={i + 1}
                size={s}
                scale={scale}
                root={scrollRef}
                highlights={highlights.filter((h) => h.page === i + 1 && (showAll || h.level !== 'high' || selectedIds.has(h.id)))}
                selectedIds={selectedIds}
                flashKey={flashKey}
                onHover={(hl, el) => setHover(hl && el ? { hl, box: el.getBoundingClientRect() } : null)}
              />
            ))}
          </div>
        )}
      </div>

      {hover && rootRef.current && <HintCard hl={hover.hl} box={hover.box} root={rootRef.current.getBoundingClientRect()} />}

      {doc && (
        <div className="pv-controls" role="toolbar" aria-label="PDF 보기">
          <span className="num pv-page">{current} / {sizes.length}</span>
          <span className="pv-sep" />
          <button type="button" className="pv-btn" onClick={() => changeZoom((z) => z / ZOOM_STEP)} disabled={zoom <= ZOOM_MIN} aria-label="축소"><IconMinus size={14} /></button>
          <button type="button" className="pv-btn pv-zoom num" onClick={() => changeZoom(() => 1)} title="폭 맞춤">{Math.round(zoom * 100)}%</button>
          <button type="button" className="pv-btn" onClick={() => changeZoom((z) => z * ZOOM_STEP)} disabled={zoom >= ZOOM_MAX} aria-label="확대"><IconPlus size={14} /></button>
        </div>
      )}
    </div>
  )
}

function PageView({ doc, pageNumber, size, scale, root, highlights, selectedIds, flashKey, onHover }: {
  doc: PDFDocumentProxy
  pageNumber: number
  size: PageSize
  scale: number
  root: RefObject<HTMLDivElement | null>
  highlights: Highlight[]
  selectedIds: Set<string>
  flashKey: number
  onHover: (hl: Highlight | null, el?: HTMLElement) => void
}) {
  const boxRef = useRef<HTMLDivElement>(null)
  const canvasRef = useRef<HTMLCanvasElement>(null)
  const [visible, setVisible] = useState(false)
  const w = size.w * scale
  const h = size.h * scale

  // 화면 근처에 오면 그린다
  useEffect(() => {
    const el = boxRef.current
    if (!el) return
    const io = new IntersectionObserver(([e]) => setVisible(e.isIntersecting), { root: root.current, rootMargin: '800px 0px' })
    io.observe(el)
    return () => io.disconnect()
  }, [root])

  // 배율이 바뀌면 다시 그린다 (잠깐 기다렸다가: 연속 확대 중 불필요한 렌더 방지)
  useEffect(() => {
    if (!visible) return
    let task: RenderTask | null = null
    let cancelled = false
    const t = setTimeout(async () => {
      const canvas = canvasRef.current
      if (!canvas) return
      const page: PDFPageProxy = await doc.getPage(pageNumber)
      if (cancelled) return
      const dpr = Math.min(window.devicePixelRatio || 1, 2)
      const vp = page.getViewport({ scale: scale * dpr })
      const off = document.createElement('canvas')
      off.width = Math.floor(vp.width)
      off.height = Math.floor(vp.height)
      task = page.render({ canvas: off, viewport: vp })
      try {
        await task.promise
        if (cancelled) return
        // 다 그린 뒤 한 번에 바꿔 깜빡임을 없앤다
        canvas.width = off.width
        canvas.height = off.height
        canvas.getContext('2d')?.drawImage(off, 0, 0)
      } catch {
        /* 취소됨 */
      }
    }, 80)
    return () => {
      cancelled = true
      clearTimeout(t)
      task?.cancel()
    }
  }, [visible, doc, pageNumber, scale])

  return (
    <div ref={boxRef} className="pv-page-box" style={{ width: w, height: h }} data-page={pageNumber}>
      <canvas ref={canvasRef} className="pv-canvas" />
      {highlights.map((hl) => {
        const sel = selectedIds.has(hl.id)
        const flash = sel && flashKey > 0
        return (
          <div
            key={flash ? `${hl.id}-${flashKey}` : hl.id}
            className={`hl-box hl-${hl.level}${flash ? ' is-flash' : ''}`}
            style={{ left: `${hl.rect.x * 100}%`, top: `${hl.rect.y * 100}%`, width: `${hl.rect.w * 100}%`, height: `${hl.rect.h * 100}%` }}
            aria-label={hl.label}
            onMouseEnter={(e) => onHover(hl, e.currentTarget)}
            onMouseLeave={() => onHover(null)}
          />
        )
      })}
      <span className="pv-page-no num">{pageNumber}</span>
    </div>
  )
}

/* ───────── 힌트 카드 (VS Code 의 문제 호버처럼) ───────── */
const LEVEL_TEXT = { mismatch: '불일치', low: '신뢰도 낮음', mid: '신뢰도 중간', high: '신뢰도 높음' } as const
const CARD_W = 300

function HintCard({ hl, box, root }: { hl: Highlight; box: DOMRect; root: DOMRect }) {
  // 박스 바로 아래, 공간이 없으면 위. 좌우는 뷰어 안에 가둔다
  const left = Math.min(Math.max(8, box.left - root.left), root.width - CARD_W - 8)
  const below = box.bottom - root.top + 6
  const placeAbove = below > root.height * 0.6
  const style = placeAbove
    ? { left, bottom: root.bottom - box.top + 6, width: CARD_W }
    : { left, top: below, width: CARD_W }
  const pct = hl.score != null ? `${Math.round(hl.score * 100)}%` : null

  return (
    <div className={`hint hint-${hl.level}`} style={style} role="tooltip">
      <div className="hint-head">
        <span className="hint-level"><span className="hint-dot" />{LEVEL_TEXT[hl.level]}{hl.level !== 'mismatch' && pct && ` ${pct}`}</span>
        <span className="hint-field">{hl.docName} · {hl.fieldLabel}</span>
      </div>
      {hl.reasons.length > 0 ? (
        hl.reasons.map((r) => (
          <div key={r.code} className="hint-reason">
            <p className="hint-title">{r.title}</p>
            <dl className="hint-cmp">
              <dt>이 값</dt>
              <dd>{hl.value}</dd>
              {r.others.map((o) => (
                <Fragment key={o.label}>
                  <dt>{o.label}</dt>
                  <dd>{o.value}</dd>
                </Fragment>
              ))}
            </dl>
          </div>
        ))
      ) : (
        <div className="hint-reason">
          <p className="hint-title">읽은 값이 정확하지 않을 수 있어요</p>
          <dl className="hint-cmp">
            <dt>읽은 값</dt>
            <dd>{hl.value}</dd>
          </dl>
        </div>
      )}
    </div>
  )
}
