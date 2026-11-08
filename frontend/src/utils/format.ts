const dateFmt = new Intl.DateTimeFormat('ko-KR', {
  year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit', hour12: false,
})

/** 2026-11-07 20:39 */
export function formatDateTime(iso: string) {
  const p = Object.fromEntries(dateFmt.formatToParts(new Date(iso)).map((x) => [x.type, x.value]))
  return `${p.year}-${p.month}-${p.day} ${p.hour}:${p.minute}`
}

/** 방금 · 5분 전 · 3시간 전 · 2일 전 · 그 이상은 날짜 */
export function formatRelative(iso: string, now = Date.now()) {
  const diff = Math.max(0, now - Date.parse(iso)) / 1000
  if (diff < 60) return '방금'
  if (diff < 3600) return `${Math.floor(diff / 60)}분 전`
  if (diff < 86400) return `${Math.floor(diff / 3600)}시간 전`
  if (diff < 86400 * 7) return `${Math.floor(diff / 86400)}일 전`
  return formatDateTime(iso).slice(0, 10)
}

/** 처리 시간: 1분 32초 */
export function formatDuration(fromIso: string, toIso: string) {
  const s = Math.max(0, Math.round((Date.parse(toIso) - Date.parse(fromIso)) / 1000))
  return s < 60 ? `${s}초` : `${Math.floor(s / 60)}분 ${s % 60}초`
}

const numFmt = new Intl.NumberFormat('ko-KR', { maximumFractionDigits: 2 })

/** 추출값 표시. 빈 값("" / 0 / null)은 '—' */
export function formatValue(v: unknown): string {
  if (v === '' || v === 0 || v == null) return '—'
  if (typeof v === 'boolean' || v === 'Y' || v === 'N') return v === true || v === 'Y' ? '예' : '아니요'
  if (typeof v === 'number') return numFmt.format(v)
  if (v === 'clear') return '선명'
  if (v === 'unclear') return '불명확'
  return String(v)
}
