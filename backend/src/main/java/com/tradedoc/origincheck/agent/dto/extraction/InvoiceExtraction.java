package com.tradedoc.origincheck.agent.dto.extraction;

import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

import java.util.List;

/**
 * 인보이스_추출.
 * <p>문서에 없는 문자열은 "", 숫자는 0, 참거짓은 false 로 온다(워크플로 규칙). null 이나 빠진 키도 같은 규칙으로 바꾼다.
 *
 * @param invoiceNo 송장번호. 'Invoice No.' 칸 값을 표기 그대로. 예: 'HA-2609-0014'. 없으면 빈 문자열.
 * @param invoiceDate YYYY-MM-DD
 * @param shipper Shipper/Exporter 회사명만. 주소는 뺀다. 없으면 빈 문자열.
 * @param consignee Consignee 회사명만. 주소는 뺀다. 없으면 빈 문자열.
 * @param portOfLoading Port of Loading 표기 그대로. 없으면 빈 문자열.
 * @param portOfDischarge Port of Discharge / Final Destination 표기 그대로. 없으면 빈 문자열.
 * @param vessel Vessel 선박명(항차 'V.xxx'는 뺀다). 없으면 빈 문자열.
 * @param incoterm 인도조건 약어와 장소. 예: 'FOB INCHEON'. 없으면 빈 문자열.
 * @param totalAmount TOTAL 금액. 통화·쉼표 빼고 숫자만. 없으면 0.
 * @param originDeclaration 한-EU 원산지신고 문안의 원산지 국가. 없으면 빈 문자열
 * @param originDeclarationAuthNo 원산지신고 문안의 인증수출자 번호(customs authorisation No.). 예: 'KR/1788/347'. 없으면 빈 문자열.
 * @param readability 핵심 값을 모두 읽었으면 clear, 아니면 unclear. 서류가 없으면 빈 문자열
 * @param items 품목 줄마다 하나
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record InvoiceExtraction(
        String invoiceNo,
        String invoiceDate,
        String shipper,
        String consignee,
        String portOfLoading,
        String portOfDischarge,
        String vessel,
        String incoterm,
        Double totalAmount,
        String originDeclaration,
        String originDeclarationAuthNo,
        String readability,
        List<InvoiceItem> items
) {
    public InvoiceExtraction {
        invoiceNo = Values.text(invoiceNo);
        invoiceDate = Values.text(invoiceDate);
        shipper = Values.text(shipper);
        consignee = Values.text(consignee);
        portOfLoading = Values.text(portOfLoading);
        portOfDischarge = Values.text(portOfDischarge);
        vessel = Values.text(vessel);
        incoterm = Values.text(incoterm);
        totalAmount = totalAmount == null ? 0.0 : totalAmount;
        originDeclaration = Values.text(originDeclaration);
        originDeclarationAuthNo = Values.text(originDeclarationAuthNo);
        readability = Values.readability(readability);
        items = items == null ? List.of() : List.copyOf(items);
    }
}
