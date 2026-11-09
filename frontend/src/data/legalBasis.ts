/**
 * '법령 확인' 패널 데이터. 불일치 코드별 판정 규칙 + 확인할 서류 + 관련 법령.
 *
 * - stage·label·rule 은 백엔드 MismatchCode.java / 워크플로 판정 프롬프트와 같은 내용이다.
 * - 법령은 법령명·주제 수준까지만 적었다. 조문 번호는 팀이 법령 원문(국가법령정보센터)에서 확인한 뒤 `article` 에 채운다.
 *   확인 안 된 조문을 화면에 단정해서 보여 주지 않기 위해서다.
 */

export interface LawRef {
  id: keyof typeof LAWS
  /** 이 코드와 관련된 주제 (예: '원산지증빙서류의 기재 사항') */
  topic: string
  /** 확인된 조문만 적는다. 예: '제11조' */
  article?: string
}

export interface CodeBasis {
  stage: 1 | 2 | 3
  label: string
  /** 판정 조건 (판정 프롬프트 그대로) */
  rule: string
  /** 담당자가 대조할 서류·칸 */
  check: string[]
  /** 어떻게 풀리는지 */
  resolution: string
  laws: LawRef[]
}

export const LAWS = {
  ftaAct: {
    name: '자유무역협정의 이행을 위한 관세법의 특례에 관한 법률',
    short: 'FTA 관세특례법',
    url: 'https://www.law.go.kr/법령/자유무역협정의이행을위한관세법의특례에관한법률',
  },
  ftaRule: {
    name: '자유무역협정의 이행을 위한 관세법의 특례에 관한 법률 시행규칙',
    short: 'FTA 관세특례법 시행규칙',
    url: 'https://www.law.go.kr/법령/자유무역협정의이행을위한관세법의특례에관한법률시행규칙',
  },
  aseanRoo: {
    name: '한-아세안 FTA 상품무역협정 부속서 3 원산지 규정',
    short: '한-아세안 FTA 원산지 규정',
    url: 'https://www.customs.go.kr/ftaportalkor/main.do',
  },
  euRoo: {
    name: '한-EU FTA 원산지 의정서',
    short: '한-EU FTA 원산지 의정서',
    url: 'https://www.customs.go.kr/ftaportalkor/main.do',
  },
} as const

export const STAGE_LABEL: Record<1 | 2 | 3, string> = { 1: '미적용', 2: '보완 요청', 3: '검토 필요' }

const proofDocs: LawRef = { id: 'ftaAct', topic: '원산지증빙서류 (원산지증명서·원산지확인서)' }
const proofItems: LawRef = { id: 'ftaRule', topic: '원산지증명서 기재 사항' }
const supplierCert: LawRef = { id: 'ftaRule', topic: '국내 공급자의 원산지확인서·포괄확인서' }
const asean: LawRef = { id: 'aseanRoo', topic: '원산지증명서(Form AK) 작성·발급' }

export const CODE_BASIS: Record<string, CodeBasis> = {
  SUP_CRITERION_NOT_MET: {
    stage: 1, label: '원산지확인서 결정기준 미충족(N)',
    rule: '원산지확인서의 결정기준 충족 여부(fulfilled)가 N',
    check: ['원산지확인서 · 충족 여부', '원산지확인서 · 결정기준'],
    resolution: '공급사가 원산지를 인정하지 않았으므로 서류를 고쳐도 특혜 근거가 없습니다. 일반 세율 신고를 안내합니다.',
    laws: [supplierCert, { id: 'ftaAct', topic: '원산지결정기준' }],
  },
  SUP_ORIGIN_MISMATCH: {
    stage: 1, label: '원산지확인서 원산지가 한국이 아님',
    rule: '원산지확인서의 원산지가 KR이 아님',
    check: ['원산지확인서 · 원산지'],
    resolution: '한국산이 아니므로 한국 FTA 특혜 대상이 아닙니다.',
    laws: [supplierCert, { id: 'ftaAct', topic: '원산지결정기준' }],
  },
  CO_INVOICE_NO_MISMATCH: {
    stage: 2, label: '원산지증명서 송장번호 ≠ 인보이스',
    rule: '원산지증명서 invoice_no 와 인보이스 invoice_no 가 다름',
    check: ['원산지증명서 · 송장번호', '인보이스 · 송장번호'],
    resolution: '지난 선적의 증명서일 수 있습니다. 발급기관에 정정·재발급을 요청합니다.',
    laws: [proofDocs, proofItems, asean],
  },
  CO_QTY_MISMATCH: {
    stage: 2, label: '원산지증명서 수량 ≠ 인보이스',
    rule: '원산지증명서 품목 수량 합계와 인보이스 품목 수량 합계가 다름',
    check: ['원산지증명서 · 품목 수량', '인보이스 · 품목 수량'],
    resolution: '발급기관에 수량 정정을 요청합니다.',
    laws: [proofItems, asean],
  },
  CO_CRITERION_MISMATCH: {
    stage: 2, label: '원산지증명서 결정기준 ≠ 원산지확인서',
    rule: '원산지증명서 결정기준이 대응표상 원산지확인서 결정기준과 다름 (WO→WO/WO-AK, CTH→CTH, CTSH·CC→CTC, BD·BU·MC·NC→RVC)',
    check: ['원산지증명서 · 결정기준', '원산지확인서 · 결정기준'],
    resolution: '원산지확인서 기준에 맞게 증명서 정정을 요청합니다.',
    laws: [proofItems, { id: 'ftaAct', topic: '원산지결정기준' }, asean],
  },
  CO_ORIGIN_MISMATCH: {
    stage: 2, label: '원산지증명서 생산국 ≠ 원산지확인서',
    rule: '원산지증명서 생산국과 원산지확인서 원산지가 다름',
    check: ['원산지증명서 · 생산국', '원산지확인서 · 원산지'],
    resolution: '어느 서류가 맞는지 공급사·발급기관에 확인 후 정정을 요청합니다.',
    laws: [proofDocs, proofItems],
  },
  CO_CONSIGNEE_MISMATCH: {
    stage: 2, label: '원산지증명서 수하인이 다른 회사',
    rule: '원산지증명서 수하인이 인보이스 수입자와 다른 회사 (법인 표기·대소문자 차이는 같은 회사로 봄)',
    check: ['원산지증명서 · 수입자', '인보이스 · 수입자'],
    resolution: '발급기관에 수하인 정정을 요청합니다.',
    laws: [proofItems, asean],
  },
  CO_DESCRIPTION_MISMATCH: {
    stage: 2, label: '원산지증명서 품명이 다른 물품',
    rule: '원산지증명서 품명이 인보이스와 다른 물품 (단어 순서·표기 차이는 같은 것으로 봄)',
    check: ['원산지증명서 · 품명', '인보이스 · 품명'],
    resolution: '발급기관에 품명 정정을 요청합니다.',
    laws: [proofItems, asean],
  },
  CO_HS_MISMATCH: {
    stage: 2, label: 'HS 앞 6자리 불일치',
    rule: 'HS 코드 앞 6자리(소호)가 서류마다 다름',
    check: ['인보이스 · HS', '원산지증명서 · HS', '원산지확인서 · HS', '수출신고필증 · HS'],
    resolution: '품목분류를 확인한 뒤 틀린 서류를 정정합니다. 결정기준이 달라질 수 있습니다.',
    laws: [proofItems, { id: 'ftaAct', topic: '원산지결정기준 (품목별 원산지기준)' }],
  },
  SUP_EXPIRED: {
    stage: 2, label: '원산지확인서 포괄확인기간 만료',
    rule: '원산지확인서 포괄확인기간(시작~종료)이 수출신고일을 포함하지 않음',
    check: ['원산지확인서 · 기간', '수출신고필증 · 신고일'],
    resolution: '공급사에 유효기간이 맞는 원산지(포괄)확인서를 다시 받습니다.',
    laws: [supplierCert],
  },
  SUP_RECIPIENT_MISMATCH: {
    stage: 2, label: '원산지확인서 공급받는 자 ≠ 수출자',
    rule: '원산지확인서 공급받는 자와 인보이스 수출자가 다름',
    check: ['원산지확인서 · 공급받는 자', '인보이스 · 수출자'],
    resolution: '공급사에 수신자를 바로 적은 원산지확인서를 요청합니다.',
    laws: [supplierCert],
  },
  DUPLICATE_DOCUMENT: {
    stage: 3, label: '같은 종류 서류가 2건 이상',
    rule: '같은 종류 문서가 2건 이상 들어 있음',
    check: ['서류별 추출값에서 같은 종류 서류 비교'],
    resolution: '어느 서류를 쓸지 담당자가 정합니다.',
    laws: [proofDocs],
  },
  EXPDEC_INVOICE_NO_MISMATCH: {
    stage: 3, label: '수출신고필증 송품장부호 ≠ 인보이스',
    rule: '수출신고필증 송품장부호와 인보이스 송장번호가 다름',
    check: ['수출신고필증 · 송품장부호', '인보이스 · 송장번호'],
    resolution: '같은 거래인지 담당자가 확인합니다.',
    laws: [proofDocs],
  },
  EXPDEC_QTY_MISMATCH: {
    stage: 3, label: '수출신고필증 수량 ≠ 인보이스',
    rule: '수출신고필증 수량 합계와 인보이스 수량 합계가 다름',
    check: ['수출신고필증 · 품목 수량', '인보이스 · 품목 수량'],
    resolution: '분할 선적·단위 차이인지 담당자가 확인합니다.',
    laws: [proofDocs],
  },
  CO_HS_MINOR_DIFF: {
    stage: 3, label: 'HS 10자리 중 뒤 4자리 차이',
    rule: '10자리 HS 가 앞 6자리는 같고 뒤 4자리가 다름',
    check: ['인보이스 · HS', '원산지확인서 · HS', '수출신고필증 · HS'],
    resolution: '협정 원산지기준은 소호(6자리) 단위라 대부분 영향이 없지만 담당자가 확인합니다.',
    laws: [{ id: 'ftaAct', topic: '원산지결정기준 (품목별 원산지기준)' }],
  },
  CO_TRANSPORT_MISMATCH: {
    stage: 3, label: '원산지증명서 운송 정보 ≠ B/L(직접운송)',
    rule: '원산지증명서 선박·양하항이 B/L 과 다르거나 B/L 에 환적항이 있음',
    check: ['원산지증명서 · 선박·양하항', 'B/L · 선박·양하항·환적항'],
    resolution: '환적이면 직접운송 요건을 충족하는지 증빙(통과선하증권 등)을 확인합니다.',
    laws: [{ id: 'ftaAct', topic: '직접운송' }, { id: 'aseanRoo', topic: '직접운송' }],
  },
  CO_ISSUE_DATE_LATE: {
    stage: 3, label: '원산지증명서 발급일이 적재일보다 3일 넘게 늦음',
    rule: '원산지증명서 발급일이 B/L 적재일보다 3일 넘게 늦고 소급발급(Issued Retroactively) 표시가 없음',
    check: ['원산지증명서 · 발급일·소급발급', 'B/L · 선적일'],
    resolution: '소급발급 표시가 필요한지 발급기관에 확인합니다.',
    laws: [asean, proofItems],
  },
  SUP_AGREEMENT_MISMATCH: {
    stage: 3, label: '원산지확인서 협정명칭 불일치',
    rule: '원산지확인서 협정명칭이 목적국 협정과 다름 (아세안 국가 → 한-아세안, EU 국가 → 한-EU)',
    check: ['원산지확인서 · 협정', '수출신고필증 · 목적국'],
    resolution: '맞는 협정으로 발급된 확인서를 공급사에 요청합니다.',
    laws: [supplierCert],
  },
  CO_CRITERION_NOT_PSR: {
    stage: 3, label: '원산지증명서 결정기준이 협정 PSR에 없음',
    rule: '원산지증명서 결정기준이 협정 품목별원산지기준(PSR)에 없음',
    check: ['원산지증명서 · 결정기준', '원산지증명서 · HS'],
    resolution: '해당 HS 의 PSR 을 확인합니다.',
    laws: [{ id: 'ftaAct', topic: '원산지결정기준 (품목별 원산지기준)' }, asean],
  },
  QTY_MISMATCH: {
    stage: 3, label: '패킹리스트 수량 ≠ 인보이스',
    rule: '패킹리스트 수량과 인보이스 수량이 다름',
    check: ['패킹리스트 · 수량', '인보이스 · 품목 수량'],
    resolution: '포장 단위 차이인지 담당자가 확인합니다.',
    laws: [],
  },
  NET_GT_GROSS: {
    stage: 3, label: '순중량 > 총중량',
    rule: '패킹리스트 순중량이 총중량보다 큼',
    check: ['패킹리스트 · 순중량·총중량'],
    resolution: '패킹리스트 기재 오류입니다. 수출자에게 정정을 요청합니다.',
    laws: [],
  },
}

/** 누락 서류별 안내 */
export const MISSING_DOC_BASIS: Record<string, { stage: 2 | 3; resolution: string }> = {
  인보이스: { stage: 2, resolution: '사내 영업 담당자에게 인보이스를 요청합니다.' },
  원산지증명서: { stage: 2, resolution: '발급기관에 발급 신청이 됐는지 확인합니다. 한-EU 건은 인보이스 원산지신고 문안으로 대신합니다.' },
  원산지확인서: { stage: 2, resolution: '국내 공급사에 원산지(포괄)확인서를 요청합니다.' },
  'B/L': { stage: 3, resolution: '운송 경로(직접운송) 확인을 위해 B/L 을 받아 둡니다.' },
  수출신고필증: { stage: 3, resolution: '수출신고 수리 후 필증을 첨부합니다.' },
}
