package com.tradedoc.origincheck.agent.dto.extraction;

import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

/**
 * BL_추출.
 * <p>문서에 없는 문자열은 "", 숫자는 0, 참거짓은 false 로 온다(워크플로 규칙). null 이나 빠진 키도 같은 규칙으로 바꾼다.
 *
 * @param blNo B/L 번호. 표기 그대로. 예: 'BL781098'. 없으면 빈 문자열.
 * @param shipper Shipper/Exporter 회사명만. 주소는 뺀다. 없으면 빈 문자열.
 * @param vessel Ocean Vessel(본선) 선박명. 항차는 뺀다. 예: 'EVER GIVEN'. 없으면 빈 문자열.
 * @param voyageNo Voyage No. 예: '8737W'. 없으면 빈 문자열.
 * @param portOfLoading Port of Loading(선적항) 표기 그대로. 예: 'BUSAN, KOREA'. 없으면 빈 문자열.
 * @param portOfDischarge Port of Discharge(양하항) 표기 그대로. 없으면 빈 문자열.
 * @param transshipmentPort 환적항. 없으면 빈 문자열
 * @param goodsDescription Description of Goods의 품명. 괄호 안 수량은 뺀다. 없으면 빈 문자열.
 * @param grossWeightKg Gross Weight kg. 쉼표·단위 빼고 숫자만. 예: '13,567.7 KGS' → 13567.7. 없으면 0.
 * @param ladenOnBoardDate 본선 적재일 YYYY-MM-DD
 * @param readability 핵심 값을 모두 읽었으면 clear, 아니면 unclear. 서류가 없으면 빈 문자열
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record BillOfLadingExtraction(
        String blNo,
        String shipper,
        String vessel,
        String voyageNo,
        String portOfLoading,
        String portOfDischarge,
        String transshipmentPort,
        String goodsDescription,
        Double grossWeightKg,
        String ladenOnBoardDate,
        String readability
) {
    public BillOfLadingExtraction {
        blNo = Values.text(blNo);
        shipper = Values.text(shipper);
        vessel = Values.text(vessel);
        voyageNo = Values.text(voyageNo);
        portOfLoading = Values.text(portOfLoading);
        portOfDischarge = Values.text(portOfDischarge);
        transshipmentPort = Values.text(transshipmentPort);
        goodsDescription = Values.text(goodsDescription);
        grossWeightKg = grossWeightKg == null ? 0.0 : grossWeightKg;
        ladenOnBoardDate = Values.text(ladenOnBoardDate);
        readability = Values.readability(readability);
    }
}
