import type { OriginCheckDetail } from '../api/types'
import { formatValue } from '../utils/format'

/**
 * 상세 화면 머리·속성 열에 쓰는 거래 요약. 추출값에서 뽑는다.
 * 목록 요약 필드(consignee·destination_country·agreement·shipped_on)를 백엔드가 만들 때도 같은 규칙을 쓰면 된다.
 */
export interface DealInfo {
  invoiceNo: string | null
  buyer: string | null
  destination: string | null
  agreement: string | null
  shippedOn: string | null
  goods: string | null
  amount: string | null
}

type Rec = Record<string, unknown>
const first = (list: unknown[] | undefined) => (list?.[0] ?? null) as Rec | null
const str = (v: unknown) => (typeof v === 'string' && v.trim() ? v.trim() : null)

/** 원산지증명서 서식 → 협정 */
const FORM_AGREEMENT: Record<string, string> = { 'FORM AK': '한-아세안', 'FORM AI': '한-인도', 'FORM E': '한-중' }

export function dealInfo(d: OriginCheckDetail): DealInfo {
  const inv = first(d.extracted.인보이스_추출)
  const co = first(d.extracted.원산지증명서_추출)
  const sup = first(d.extracted.원산지확인서_추출)
  const bl = first(d.extracted.BL_추출)
  const exp = first(d.extracted.수출신고필증_추출)
  const items = (inv?.items as Rec[] | undefined) ?? []
  const supItem = ((sup?.items as Rec[] | undefined) ?? [])[0]
  const total = typeof inv?.total_amount === 'number' && inv.total_amount > 0 ? formatValue(inv.total_amount) : null

  return {
    invoiceNo: d.judge?.invoice_no ?? str(inv?.invoice_no),
    buyer: str(inv?.consignee) ?? str(co?.consignee),
    destination: str(exp?.destination_country) ?? str(co?.importing_country),
    agreement: str(supItem?.agreement) ?? FORM_AGREEMENT[(str(co?.form_type) ?? '').toUpperCase()] ?? null,
    shippedOn: str(bl?.laden_on_board_date) ?? str(co?.departure_date),
    goods: items.length ? `${str(items[0].description) ?? '품목'}${items.length > 1 ? ` 외 ${items.length - 1}건` : ''}` : null,
    amount: total ? `${total}${str(inv?.incoterm) ? ` (${str(inv?.incoterm)})` : ''}` : null,
  }
}
