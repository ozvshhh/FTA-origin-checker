import type { ExtractNode } from '../api/types'

/** 서류별 추출값 화면 표시용: 탭 이름, 필드 라벨, 품목표 열 (화면명세서 §5.2 F) */
export interface DocDef {
  node: ExtractNode
  name: string
  fields: [key: string, label: string][]
  items?: [key: string, label: string][]
}

export const DOC_DEFS: DocDef[] = [
  {
    node: '인보이스_추출', name: '인보이스',
    fields: [
      ['invoice_no', '송장번호'], ['invoice_date', '송장일자'], ['shipper', '수출자'], ['consignee', '수입자'],
      ['port_of_loading', '선적항'], ['port_of_discharge', '양하항'], ['vessel', '선박'], ['incoterm', '인코텀즈'],
      ['total_amount', '총액'], ['origin_declaration', '원산지신고 문안'], ['origin_declaration_auth_no', '인증수출자번호'],
      ['readability', '판독'],
    ],
    items: [['description', '품명'], ['hs_code', 'HS'], ['quantity', '수량'], ['amount', '금액']],
  },
  {
    node: '원산지증명서_추출', name: '원산지증명서',
    fields: [
      ['reference_no', '증명서번호'], ['form_type', '서식'], ['exporter', '수출자'], ['consignee', '수입자'],
      ['departure_date', '출항일'], ['vessel', '선박'], ['port_of_discharge', '양하항'], ['invoice_no', '송장번호'],
      ['invoice_date', '송장일자'], ['exporter_country', '생산국'], ['importing_country', '수입국'], ['issue_date', '발급일'],
      ['issued_retroactively', '소급발급'], ['readability', '판독'],
    ],
    items: [['description', '품명'], ['quantity', '수량'], ['hs_code', 'HS'], ['origin_criterion', '결정기준'], ['gross_weight_kg', '총중량'], ['fob_value', 'FOB']],
  },
  {
    node: '원산지확인서_추출', name: '원산지확인서',
    fields: [['issue_no', '발급번호'], ['supplier', '공급자'], ['recipient', '공급받는 자'], ['issue_date', '발급일'], ['readability', '판독']],
    items: [['agreement', '협정'], ['hs_code', 'HS'], ['description', '품명'], ['origin_criterion', '결정기준'], ['fulfilled', '충족'], ['origin_country', '원산지'], ['period_from', '시작'], ['period_to', '종료']],
  },
  {
    node: 'BL_추출', name: 'B/L',
    fields: [
      ['bl_no', 'B/L 번호'], ['shipper', '송하인'], ['vessel', '선박'], ['voyage_no', '항차'], ['port_of_loading', '선적항'],
      ['port_of_discharge', '양하항'], ['transshipment_port', '환적항'], ['goods_description', '품명'], ['gross_weight_kg', '총중량(kg)'],
      ['laden_on_board_date', '선적일'], ['readability', '판독'],
    ],
  },
  {
    node: '수출신고필증_추출', name: '수출신고필증',
    fields: [
      ['declaration_no', '신고번호'], ['declaration_date', '신고일'], ['acceptance_date', '수리일'], ['exporter', '수출자'],
      ['manufacturer', '제조자'], ['buyer', '구매자'], ['destination_country', '목적국'], ['vessel', '선박'],
      ['invoice_no', '송품장부호'], ['payment_amount', '결제금액'], ['readability', '판독'],
    ],
    items: [['hs_code', 'HS'], ['description', '품명'], ['quantity', '수량'], ['origin_country', '원산지']],
  },
  {
    node: '패킹리스트_추출', name: '패킹리스트',
    fields: [
      ['invoice_no', '송장번호'], ['consignee', '수입자'], ['goods_description', '품명'], ['quantity', '수량'], ['cartons', '포장수'],
      ['net_weight_kg', '순중량(kg)'], ['gross_weight_kg', '총중량(kg)'], ['readability', '판독'],
    ],
  },
]
