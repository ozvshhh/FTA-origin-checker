import { useCallback, useEffect, useLayoutEffect, useRef, useState, type CSSProperties, type ReactNode } from 'react'
import { IconFile } from './icons'

/**
 * 처음 로그인한 사용자에게 한 번 보여 주는 코치마크.
 * 화면 전체를 어둡게 덮고, 실제 버튼만 뚫어 밝게 보인 뒤 점선 화살표로 "○○를 눌러 ~하세요"를 단다.
 * 1장 = 왼쪽 목록, 2장 = 심사 상세 + 원본 PDF 패널(열린 심사가 없으면 예시 한 건을 연다). 아무 데나 누르면 다음 장.
 * 대상은 화면 요소의 data-tour 속성으로 찾는다. 안 보이는 대상은 건너뛴다(좁은 화면, 빈 목록 등).
 */
const KEY = 'origin-check.tutorial.v1'

/** 이 사용자가 튜토리얼을 이미 봤는가. [백엔드 추가 후보] 지금은 브라우저에만 기억한다 */
export function tutorialSeen(userId: number | string) {
  try {
    return localStorage.getItem(`${KEY}.${userId}`) === 'done'
  } catch {
    return true // 저장소를 못 쓰면 매번 띄우지 않는다
  }
}
function markSeen(userId: number | string) {
  try {
    localStorage.setItem(`${KEY}.${userId}`, 'done')
  } catch {
    /* noop */
  }
}

type Place = 'right' | 'left' | 'below' | 'above'
type Align = 'start' | 'center' | 'end'
interface Mark {
  /** data-tour 값. key 로도 쓴다 */
  tour: string
  /** data-tour 대신 직접 찾을 때 (예: PDF 위 형광펜) */
  find?: () => HTMLElement | null
  /** 대상 위에 마우스 커서 그림을 얹는다 */
  cursor?: boolean
  /** left 일 때 글을 이 요소 바깥(왼쪽)까지 빼서 어두운 곳에 둔다 */
  outside?: string
  /** 글을 화살표 시작점보다 이만큼 아래에 둔다 (다른 글과 겹칠 때) */
  drop?: number
  place: Place
  /** below/above 일 때 글 상자를 화살표 기준 어느 쪽으로 펼칠지 */
  align?: Align
  /** right/left 일 때 대상의 세로 몇 % 지점에서 화살표를 뺄지 (기본 50%) */
  at?: number
  /** 화살표 길이 */
  len?: number
  pad?: number
  radius?: number
  /** 없으면 구멍만 뚫는다 */
  text?: ReactNode
}
interface Page {
  key: 'list' | 'detail'
  marks: Mark[]
  center?: ReactNode
}

/** 원본 PDF 패널에서 지금 보이는 형광펜 하나 (불일치 → 신뢰도 낮음 → 중간 순) */
function findHighlight(): HTMLElement | null {
  const panel = document.querySelector('[data-tour="pdf"]')?.getBoundingClientRect()
  if (!panel) return null
  const rank = (el: Element) => ['hl-mismatch', 'hl-low', 'hl-mid'].findIndex((c) => el.classList.contains(c))
  const shown = Array.from(document.querySelectorAll<HTMLElement>('[data-tour="pdf"] .hl-box')).filter((el) => {
    const r = el.getBoundingClientRect()
    return rank(el) >= 0 && r.top >= panel.top + 40 && r.bottom <= panel.bottom - 60
  })
  return shown.sort((a, b) => rank(a) - rank(b))[0] ?? null
}

const Hi = ({ children }: { children: ReactNode }) => <em className="cm-hi">{children}</em>

const PAGES: Page[] = [
  {
    key: 'list',
    marks: [
      { tour: 'new', place: 'right', len: 64, radius: 99, text: <><Hi>+</Hi>를 눌러<br />심사 생성</> },
      { tour: 'views', place: 'right', len: 48, text: <>처리가 끝난 건은<br /><Hi>완료</Hi> 탭에 모여요</> },
      {
        tour: 'list', place: 'right', at: 0.22, len: 56, pad: 0, radius: 0,
        text: <>급한 일부터 묶여 있어요<br /><Hi>조치 필요</Hi> · 검토 필요 · 점검 중 · 특혜 적용</>,
      },
      { tour: 'help', place: 'right', len: 72, radius: 99, text: <>이 안내는 <Hi>?</Hi>로 다시 볼 수 있어요</> },
    ],
    center: (
      <>
        <span className="cm-drop" aria-hidden><IconFile size={26} /></span>
        <p className="cm-center-title">
          서류 PDF를 화면 아무 데나<br /><Hi>끌어다 놓으면</Hi> 점검이 시작돼요
        </p>
        <p className="cm-center-sub">서류별로 나뉜 파일도, 하나로 합친 PDF도 괜찮아요</p>
      </>
    ),
  },
  {
    key: 'detail',
    marks: [
      { tour: 'nav', place: 'left', len: 56, text: <><Hi>↑ ↓</Hi> 로 이전 · 다음 심사</> },
      { tour: 'done', place: 'below', align: 'end', len: 56, text: <>처리했으면 <Hi>완료 처리</Hi><br />다음 건이 바로 열려요 (E)</> },
      { tour: 'verdict', place: 'right', len: 72, text: <>AI가 낸 <Hi>1차 판정</Hi><br />최종 판단은 담당자가 해요</> },
      { tour: 'issue', place: 'left', len: 56, text: <><Hi>원본</Hi>은 PDF의 그 자리로,<br /><Hi>근거</Hi>는 판정 규칙과 법령으로</> },
      { tour: 'pdf', place: 'left', pad: 0, radius: 10 },
      {
        tour: 'hl', find: findHighlight, cursor: true, place: 'left', outside: '[data-tour="pdf"]', drop: 170, pad: 3, radius: 3,
        text: (
          <>
            원본 PDF에서 칠해진 곳에 <Hi>마우스를 올리면</Hi><br />
            왜 문제인지 바로 보여요
            <span className="cm-legend">
              <span><i className="cm-sw cm-sw-red" />불일치</span>
              <span><i className="cm-sw cm-sw-orange" />신뢰도 낮음</span>
              <span><i className="cm-sw cm-sw-yellow" />신뢰도 중간</span>
            </span>
          </>
        ),
      },
    ],
  },
]

interface Placed {
  mark: Mark
  hole: { x: number; y: number; w: number; h: number; r: number }
  arrow: string | null
  head: string | null
  label: CSSProperties | null
}

/** 대상 하나의 구멍·화살표·글 위치를 잰다 */
function place(mark: Mark): Placed | null {
  const el = mark.find ? mark.find() : document.querySelector<HTMLElement>(`[data-tour="${mark.tour}"]`)
  if (!el) return null
  const r = el.getBoundingClientRect()
  const vw = window.innerWidth
  const vh = window.innerHeight
  if (r.width === 0 || r.height === 0 || r.bottom < 0 || r.top > vh || r.right < 0 || r.left > vw) return null
  // 다른 것(예: 좁은 화면에서 위로 덮인 PDF 패널)에 가려졌으면 건너뛴다
  const top = document.elementsFromPoint(r.left + r.width / 2, r.top + r.height / 2).find((n) => !n.closest('.coach'))
  if (top && !el.contains(top) && !top.contains(el)) return null
  const pad = mark.pad ?? 6
  const x = Math.max(0, r.left - pad)
  const y = Math.max(0, r.top - pad)
  const w = Math.min(vw, r.right + pad) - x
  const h = Math.min(vh, r.bottom + pad) - y
  const rad = Math.min(mark.radius ?? 8, h / 2, w / 2)
  if (!mark.text) return { mark, hole: { x, y, w, h, r: rad }, arrow: null, head: null, label: null }
  const len = mark.len ?? 56
  const gap = 8
  const align = mark.align ?? 'center'

  // s = 대상 쪽 끝(화살촉), e = 글 쪽 끝. c = 휘는 정도를 주는 조절점
  let s: [number, number], e: [number, number], c: [number, number]
  let label: CSSProperties
  const at = mark.at ?? 0.5
  switch (mark.place) {
    case 'right': {
      s = [x + w + gap, y + h * at]
      e = [s[0] + len, s[1] + 14]
      c = [s[0] + len * 0.45, s[1] - 16]
      label = { left: e[0] + 10, top: e[1], transform: 'translateY(-50%)', textAlign: 'left' }
      break
    }
    case 'left': {
      s = [x - gap, y + h * at]
      const box = mark.outside ? document.querySelector(mark.outside)?.getBoundingClientRect() : null
      const far = box ? Math.max(len, s[0] - box.left + 28) : len
      const drop = mark.drop ?? 14
      e = [s[0] - far, s[1] + drop]
      c = [s[0] - far * 0.35, s[1] + (mark.drop ? drop * 0.9 : -16)]
      label = { left: e[0] - 10, top: e[1], transform: 'translate(-100%, -50%)', textAlign: 'right' }
      break
    }
    case 'below':
    case 'above': {
      const down = mark.place === 'below'
      const side = align === 'end' ? -1 : align === 'start' ? 1 : 0
      s = [x + w / 2, down ? y + h + gap : y - gap]
      e = [s[0] + side * 18, s[1] + (down ? len : -len)]
      c = [s[0] + (side || 1) * -14, s[1] + (down ? len : -len) * 0.5]
      const tx = align === 'end' ? 'calc(-100% + 28px)' : align === 'start' ? '-28px' : '-50%'
      label = {
        left: e[0],
        top: e[1],
        transform: `translate(${tx}, ${down ? '8px' : 'calc(-100% - 8px)'})`,
        textAlign: align === 'end' ? 'right' : align === 'start' ? 'left' : 'center',
      }
      break
    }
  }
  // 화살촉: 조절점 → 끝점 방향
  const dx = s[0] - c[0]
  const dy = s[1] - c[1]
  const d = Math.hypot(dx, dy) || 1
  const ux = dx / d
  const uy = dy / d
  const k = 8
  const wing = (a: number) => {
    const cos = Math.cos(a)
    const sin = Math.sin(a)
    return [s[0] - k * (ux * cos - uy * sin), s[1] - k * (uy * cos + ux * sin)]
  }
  const [l1x, l1y] = wing(0.5)
  const [l2x, l2y] = wing(-0.5)
  const f = (n: number) => n.toFixed(1)
  return {
    mark,
    hole: { x, y, w, h, r: rad },
    arrow: `M${f(e[0])},${f(e[1])} Q${f(c[0])},${f(c[1])} ${f(s[0])},${f(s[1])}`,
    head: `M${f(l1x)},${f(l1y)} L${f(s[0])},${f(s[1])} L${f(l2x)},${f(l2y)}`,
    label,
  }
}

export function Tutorial({ userId, onClose, onStart, sampleId, onShowDetail, hasChecks }: {
  userId: number | string
  onClose: () => void
  /** 심사가 하나도 없을 때 마지막 버튼 = 심사 생성 */
  onStart: () => void
  /** 상세 장에서 보여 줄 예시 심사 (지금 열린 상세가 없을 때 연다) */
  sampleId: number | null
  /** 상세 장에 들어올 때: 판정이 나온 심사를 원본 PDF 패널과 함께 띄운다 */
  onShowDetail: () => void
  hasChecks: boolean
}) {
  // 상세 장은 판정이 나온 심사가 있을 때만
  const pages = PAGES.filter((p) => p.key === 'list' || sampleId != null || document.querySelector('[data-tour="verdict"]'))
  const [i, setI] = useState(0)
  const page = pages[Math.min(i, pages.length - 1)]
  const last = i >= pages.length - 1
  const [placed, setPlaced] = useState<Placed[]>([])
  const [center, setCenter] = useState<{ x: number; y: number } | null>(null)

  const close = useCallback(() => {
    markSeen(userId)
    onClose()
  }, [userId, onClose])
  const finish = useCallback(() => {
    close()
    if (!hasChecks) onStart()
  }, [close, hasChecks, onStart])
  const next = useCallback(() => (last ? finish() : setI((n) => n + 1)), [last, finish])
  const prev = useCallback(() => setI((n) => Math.max(0, n - 1)), [])

  // 상세 장에 들어올 때 한 번: 심사 상세 + 원본 PDF 패널을 띄운다
  const showDetail = useRef(onShowDetail)
  useEffect(() => {
    showDetail.current = onShowDetail
  })
  useEffect(() => {
    if (page.key === 'detail') showDetail.current()
  }, [page.key])

  // 상세 장: 본문은 맨 위로, PDF 형광펜 하나에 마우스를 올린 것처럼 이유 카드를 띄워 둔다
  useEffect(() => {
    if (page.key !== 'detail') return
    document.querySelector('.detail-scroll')?.scrollTo({ top: 0 })
    let hovered: Element | null = null
    let last = ''
    const t = window.setInterval(() => {
      const el = findHighlight()
      if (!el || hovered === el) return
      // PDF 가 그 자리로 스크롤을 마칠 때까지 기다렸다가 (위치가 두 번 같으면)
      const r = el.getBoundingClientRect()
      const sig = `${Math.round(r.x)},${Math.round(r.y)}`
      if (sig !== last || r.top < 0 || r.bottom > innerHeight) {
        last = sig
        return
      }
      if (hovered) hovered.dispatchEvent(new MouseEvent('mouseout', { bubbles: true, relatedTarget: document.body }))
      el.dispatchEvent(new MouseEvent('mouseover', { bubbles: true, relatedTarget: document.body }))
      hovered = el
    }, 300)
    return () => {
      window.clearInterval(t)
      hovered?.dispatchEvent(new MouseEvent('mouseout', { bubbles: true, relatedTarget: document.body }))
    }
  }, [page.key])

  // 대상 위치 재기: 화면 크기·스크롤이 바뀌거나 데이터가 늦게 그려져도 따라가게 짧게 다시 잰다
  useLayoutEffect(() => {
    let last = ''
    const measure = () => {
      const next = page.marks.map(place).filter((p): p is Placed => p != null)
      const ws = document.querySelector('.workspace')?.getBoundingClientRect()
      // 가운데 문구는 오른쪽 작업 영역 기준. 좁은 화면에서 작업 영역이 숨으면 목록을 가리지 않게 빼 둔다
      const c = ws && ws.width > 0 ? { x: ws.left + ws.width / 2, y: ws.top + ws.height * (page.key === 'list' ? 0.56 : 0.86) } : null
      const sig = JSON.stringify([next.map((p) => p.hole), c])
      if (sig === last) return
      last = sig
      setPlaced(next)
      setCenter(c)
    }
    measure()
    const t = window.setInterval(measure, 250)
    window.addEventListener('resize', measure)
    return () => {
      window.clearInterval(t)
      window.removeEventListener('resize', measure)
    }
  }, [page])

  useEffect(() => {
    const onKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape') close()
      else if (e.key === 'ArrowRight' || e.key === 'Enter' || e.key === ' ') next()
      else if (e.key === 'ArrowLeft') prev()
      // 덮개가 떠 있는 동안 뒤 화면 단축키(↑↓·E 등)는 막는다
      e.preventDefault()
      e.stopPropagation()
    }
    window.addEventListener('keydown', onKey, true)
    return () => window.removeEventListener('keydown', onKey, true)
  }, [close, next, prev])

  return (
    <div className="coach" role="dialog" aria-modal="true" aria-label="사용법 안내" onClick={next}>
      <svg className="coach-svg" width="100%" height="100%" aria-hidden>
        <defs>
          <mask id="coach-mask">
            <rect width="100%" height="100%" fill="white" />
            {placed.map((p) => (
              <rect key={p.mark.tour} x={p.hole.x} y={p.hole.y} width={p.hole.w} height={p.hole.h} rx={p.hole.r} fill="black" />
            ))}
          </mask>
        </defs>
        <rect className="coach-dim" width="100%" height="100%" mask="url(#coach-mask)" />
        {placed.map((p) => (
          <g key={p.mark.tour}>
            {p.arrow && <rect className="coach-ring" x={p.hole.x} y={p.hole.y} width={p.hole.w} height={p.hole.h} rx={p.hole.r} />}
            {/* 밝은 곳(PDF 위)을 지날 때도 보이게 어두운 테두리를 먼저 깐다 */}
            {p.arrow && <path className="coach-halo" d={p.arrow} />}
            {p.head && <path className="coach-halo" d={p.head} />}
            {p.arrow && <path className="coach-arrow" d={p.arrow} />}
            {p.head && <path className="coach-head" d={p.head} />}
            {p.mark.cursor && (
              <path
                className="coach-cursor"
                transform={`translate(${p.hole.x + p.hole.w * 0.6} ${p.hole.y + p.hole.h * 0.55})`}
                d="M0 0 L0 17 L4.5 13 L7.5 20 L10.5 18.6 L7.6 11.8 L13.5 11.8 Z"
              />
            )}
          </g>
        ))}
      </svg>

      {placed.map((p) => p.label && (
        <p key={p.mark.tour} className="cm-label" style={p.label}>{p.mark.text}</p>
      ))}

      {page.center && center && (
        <div className="cm-center" style={{ left: center.x, top: center.y }}>{page.center}</div>
      )}

      <footer className="cm-foot" onClick={(e) => e.stopPropagation()}>
        {pages.length > 1 && (
          <div className="cm-dots" aria-hidden>
            {pages.map((p, n) => <i key={p.key} className={n === i ? 'is-on' : undefined} />)}
          </div>
        )}
        <div className="cm-actions">
          {i > 0 && <button type="button" className="cm-link cm-sub" onClick={prev}>이전</button>}
          {!last && <button type="button" className="cm-link cm-sub" onClick={close}>건너뛰기</button>}
          <button type="button" className="cm-link" onClick={next} autoFocus>
            {last ? (hasChecks ? '시작하기' : '심사 생성') : '다음'} <span aria-hidden>›</span>
          </button>
        </div>
      </footer>
    </div>
  )
}
