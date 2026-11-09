package com.tradedoc.origincheck.agent.dto;

import java.util.Optional;

/**
 * 판정 프롬프트가 내는 불일치 코드. stage 1=미적용, 2=보완 요청, 3=검토 필요.
 * label 은 화면 표시용 짧은 문구, description 은 판정 조건이다.
 * 응답의 mismatch_codes 는 문자열로 받는다(모르는 코드가 와도 응답이 깨지지 않게). 이 enum 은 조회용이다.
 */
public enum MismatchCode {
    // 1단계: 미적용
    SUP_CRITERION_NOT_MET(1, "원산지확인서 결정기준 미충족(N)", "원산지확인서 충족 여부(fulfilled)가 N"),
    SUP_ORIGIN_MISMATCH(1, "원산지확인서 원산지가 한국이 아님", "원산지확인서 원산지가 KR이 아님"),
    // 2단계: 보완 요청
    CO_INVOICE_NO_MISMATCH(2, "원산지증명서 송장번호 ≠ 인보이스", "원산지증명서 invoice_no와 인보이스 invoice_no가 다름"),
    CO_QTY_MISMATCH(2, "원산지증명서 수량 ≠ 인보이스", "원산지증명서 품목 수량 합계와 인보이스 품목 수량 합계가 다름"),
    CO_CRITERION_MISMATCH(2, "원산지증명서 결정기준 ≠ 원산지확인서", "원산지증명서 결정기준이 대응표상 원산지확인서 결정기준과 다름"),
    CO_ORIGIN_MISMATCH(2, "원산지증명서 생산국 ≠ 원산지확인서", "원산지증명서 생산국과 원산지확인서 원산지가 다름"),
    CO_CONSIGNEE_MISMATCH(2, "원산지증명서 수하인이 다른 회사", "원산지증명서 수하인이 다른 회사"),
    CO_DESCRIPTION_MISMATCH(2, "원산지증명서 품명이 다른 물품", "원산지증명서 품명이 다른 물품"),
    CO_HS_MISMATCH(2, "HS 앞 6자리 불일치", "HS 코드 앞 6자리가 서류마다 다름"),
    SUP_EXPIRED(2, "원산지확인서 포괄확인기간 만료", "원산지확인서 포괄확인기간이 수출신고일을 포함하지 않음"),
    SUP_RECIPIENT_MISMATCH(2, "원산지확인서 공급받는 자 ≠ 수출자", "원산지확인서 공급받는 자와 인보이스 수출자가 다름"),
    // 3단계: 검토 필요
    DUPLICATE_DOCUMENT(3, "같은 종류 서류가 2건 이상", "같은 종류 문서가 2건 이상"),
    EXPDEC_INVOICE_NO_MISMATCH(3, "수출신고필증 송품장부호 ≠ 인보이스", "수출신고필증 송품장부호와 인보이스 invoice_no가 다름"),
    EXPDEC_QTY_MISMATCH(3, "수출신고필증 수량 ≠ 인보이스", "수출신고필증 수량 합계와 인보이스 수량 합계가 다름"),
    CO_HS_MINOR_DIFF(3, "HS 10자리 중 뒤 4자리 차이", "10자리 HS가 앞 6자리는 같고 뒤 4자리가 다름"),
    CO_TRANSPORT_MISMATCH(3, "원산지증명서 운송 정보 ≠ B/L(직접운송)", "원산지증명서 선박·양하항이 B/L과 다르거나 B/L에 환적항이 있음"),
    CO_ISSUE_DATE_LATE(3, "원산지증명서 발급일이 적재일보다 3일 넘게 늦음", "원산지증명서 발급일이 적재일보다 3일 넘게 늦고 소급발급 표시 없음"),
    SUP_AGREEMENT_MISMATCH(3, "원산지확인서 협정명칭 불일치", "원산지확인서 협정명칭이 목적국 협정 대응표와 다름"),
    CO_CRITERION_NOT_PSR(3, "원산지증명서 결정기준이 협정 PSR에 없음", "원산지증명서 결정기준이 협정 품목별원산지기준(PSR)에 없음"),
    QTY_MISMATCH(3, "패킹리스트 수량 ≠ 인보이스", "패킹리스트 수량과 인보이스 수량이 다름"),
    NET_GT_GROSS(3, "순중량 > 총중량", "패킹리스트 순중량이 총중량보다 큼");

    private final int stage;
    private final String label;
    private final String description;

    MismatchCode(int stage, String label, String description) {
        this.stage = stage;
        this.label = label;
        this.description = description;
    }

    public int stage() {
        return stage;
    }

    public String label() {
        return label;
    }

    public String description() {
        return description;
    }

    public static Optional<MismatchCode> find(String code) {
        if (code == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(valueOf(code.trim()));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
