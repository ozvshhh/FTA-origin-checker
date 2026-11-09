import { lookupCountry } from '../data/countries'

/**
 * 국기 + 국가명. mode="ko" 는 한글 이름(원문은 마우스를 올리면), mode="raw" 는 서류 원문 그대로(추출값 표).
 * 모르는 나라는 원문만 보여 준다.
 */
export function Country({ value, mode = 'ko' }: { value: string | null | undefined; mode?: 'ko' | 'raw' }) {
  if (!value) return null
  const c = lookupCountry(value)
  if (!c) return <span className="country">{value}</span>
  return (
    <span className="country" title={mode === 'ko' && value !== c.ko ? value : undefined}>
      <img className="flag" src={c.flag} alt="" width={16} height={12} />
      {mode === 'ko' ? c.ko : value}
    </span>
  )
}

/** 국가가 들어가는 추출 항목 (서류별 추출값 표에서 국기를 붙인다) */
export const COUNTRY_FIELDS = new Set(['exporter_country', 'importing_country', 'destination_country', 'origin_country'])
