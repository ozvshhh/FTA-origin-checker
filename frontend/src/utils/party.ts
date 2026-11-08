/** 목록·머리 표시용: 법인 표기를 떼고 짧게 (KL HOME SUPPLY SDN. BHD. → KL HOME SUPPLY) */
const SUFFIX = /[\s,]+(CO\.?,?\s*LTD\.?|CORP(ORATION)?\.?|INC\.?|LTD\.?|LIMITED|SDN\.?\s*BHD\.?|BHD\.?|JSC|GMBH|AG|S\.?A\.?|B\.?V\.?|PTE\.?\s*LTD\.?|LLC|PLC|주식회사|\(주\))\s*$/i

export function shortCompany(name: string | null | undefined): string | null {
  if (!name) return null
  let s = name.trim().replace(/^(PT|CV|㈜|\(주\)|주식회사)\s+/i, '')
  for (let i = 0; i < 2; i++) s = s.replace(SUFFIX, '').trim()
  return s || name.trim()
}

/** 대표 품목 + 외 n건 */
export function itemLabel(primary: string | null | undefined, count?: number | null): string | null {
  if (!primary) return null
  return count && count > 1 ? `${primary} 외 ${count - 1}건` : primary
}

/** 사용자가 넣은 관리번호·메모. 파일명과 같으면(비워서 자동 저장된 경우) 없는 것으로 본다 */
export function memoOf(title: string, fileName: string): string | null {
  return title && title !== fileName ? title : null
}
