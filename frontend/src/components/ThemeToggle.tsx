import { useEffect, useState } from 'react'

/**
 * 라이트/다크 전환 버튼. 기본은 라이트(화이트) 모드이고, 누르면 그 선택을 기억한다.
 * <html data-theme="light|dark"> 를 바꾸면 tokens.css 의 의미 토큰이 따라 바뀐다.
 * 첫 화면 깜빡임을 막으려고 index.html 에서도 같은 키를 먼저 읽는다.
 */
const STORAGE_KEY = 'origin-check.theme'
type Theme = 'light' | 'dark'

/** OS 설정과 관계없이 처음엔 라이트. OS 를 따르게 하려면 index.html 과 여기를 함께 바꾼다 */
const DEFAULT_THEME: Theme = 'light'

function readSaved(): Theme | null {
  try {
    const v = localStorage.getItem(STORAGE_KEY)
    return v === 'light' || v === 'dark' ? v : null
  } catch {
    return null
  }
}

export function ThemeToggle() {
  const [theme, setTheme] = useState<Theme>(() => readSaved() ?? DEFAULT_THEME)

  useEffect(() => {
    document.documentElement.dataset.theme = theme
  }, [theme])

  const toggle = () => {
    const next: Theme = theme === 'dark' ? 'light' : 'dark'
    setTheme(next)
    try {
      localStorage.setItem(STORAGE_KEY, next)
    } catch {
      /* 저장 불가 환경이면 이번 방문 동안만 유지 */
    }
  }

  const label = theme === 'dark' ? '라이트 모드로 전환' : '다크 모드로 전환'
  return (
    <button type="button" className="btn-icon btn-ghost" onClick={toggle} aria-label={label} title={label}>
      {theme === 'dark' ? <SunIcon /> : <MoonIcon />}
    </button>
  )
}

const p = { width: 18, height: 18, viewBox: '0 0 24 24', fill: 'none', stroke: 'currentColor', strokeWidth: 2, strokeLinecap: 'round', strokeLinejoin: 'round', 'aria-hidden': true } as const
const SunIcon = () => (
  <svg {...p}>
    <circle cx="12" cy="12" r="4" />
    <path d="M12 2v2M12 20v2M4.9 4.9l1.4 1.4M17.7 17.7l1.4 1.4M2 12h2M20 12h2M4.9 19.1l1.4-1.4M17.7 6.3l1.4-1.4" />
  </svg>
)
const MoonIcon = () => (
  <svg {...p}>
    <path d="M21 12.8A9 9 0 1 1 11.2 3a7 7 0 0 0 9.8 9.8z" />
  </svg>
)
