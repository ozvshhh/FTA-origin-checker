package com.tradedoc.origincheck.agent.dto;

import java.util.List;

/** Studio 워크플로의 노드 이름. 응답 키와 Upstage 출력의 model 이름에 그대로 쓰인다. */
public final class ExtractNode {
    public static final String INVOICE = "인보이스_추출";
    public static final String PACKING_LIST = "패킹리스트_추출";
    public static final String BILL_OF_LADING = "BL_추출";
    public static final String CERTIFICATE_OF_ORIGIN = "원산지증명서_추출";
    public static final String ORIGIN_CONFIRMATION = "원산지확인서_추출";
    public static final String EXPORT_DECLARATION = "수출신고필증_추출";

    public static final List<String> ALL = List.of(
            INVOICE, PACKING_LIST, BILL_OF_LADING, CERTIFICATE_OF_ORIGIN, ORIGIN_CONFIRMATION, EXPORT_DECLARATION);

    /** 판정 노드 */
    public static final String JUDGE = "원산지_판정";

    private ExtractNode() {
    }
}
