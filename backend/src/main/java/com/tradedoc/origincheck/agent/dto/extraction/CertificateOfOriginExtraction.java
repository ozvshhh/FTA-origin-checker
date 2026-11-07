package com.tradedoc.origincheck.agent.dto.extraction;

import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

import java.util.List;

/**
 * 원산지증명서_추출 (FORM AK 등).
 * <p>문서에 없는 문자열은 "", 숫자는 0, 참거짓은 false 로 온다(워크플로 규칙). null 이나 빠진 키도 같은 규칙으로 바꾼다.
 *
 * @param referenceNo 증명서 번호(Reference No.). 표기 그대로. 예: 'AK-26-00001', 'AK-26-00010-R1'. 없으면 빈 문자열.
 * @param formType 서식 이름. 예: 'FORM AK'. 없으면 빈 문자열.
 * @param exporter 1번 Goods consigned from의 회사명만. 없으면 빈 문자열.
 * @param consignee 2번 Goods consigned to의 회사명만. 없으면 빈 문자열.
 * @param departureDate 3번 Departure date. YYYY-MM-DD. 없으면 빈 문자열.
 * @param vessel 3번 Vessel's name (항차 'V.xxx'는 뺀다). 없으면 빈 문자열.
 * @param portOfDischarge 3번 Port of Discharge. 없으면 빈 문자열.
 * @param invoiceNo 10번 칸 송장번호(원문 값)
 * @param invoiceDate 10번 칸의 송장 날짜. YYYY-MM-DD. 없으면 빈 문자열.
 * @param exporterCountry 11번 신고문 'produced in' 뒤 국가. 예: 'REPUBLIC OF KOREA'. 없으면 빈 문자열.
 * @param importingCountry 11번 신고문 'exported to' 뒤 국가. 예: 'VIETNAM'. 없으면 빈 문자열.
 * @param issueDate 12번 Certification 칸 발급일
 * @param issuedRetroactively 'ISSUED RETROACTIVELY' 표시가 있으면 true, 없으면 false.
 * @param readability 핵심 값을 모두 읽었으면 clear, 아니면 unclear. 서류가 없으면 빈 문자열
 * @param items 품목 줄마다 하나
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record CertificateOfOriginExtraction(
        String referenceNo,
        String formType,
        String exporter,
        String consignee,
        String departureDate,
        String vessel,
        String portOfDischarge,
        String invoiceNo,
        String invoiceDate,
        String exporterCountry,
        String importingCountry,
        String issueDate,
        Boolean issuedRetroactively,
        String readability,
        List<CertificateOfOriginItem> items
) {
    public CertificateOfOriginExtraction {
        referenceNo = Values.text(referenceNo);
        formType = Values.text(formType);
        exporter = Values.text(exporter);
        consignee = Values.text(consignee);
        departureDate = Values.text(departureDate);
        vessel = Values.text(vessel);
        portOfDischarge = Values.text(portOfDischarge);
        invoiceNo = Values.text(invoiceNo);
        invoiceDate = Values.text(invoiceDate);
        exporterCountry = Values.text(exporterCountry);
        importingCountry = Values.text(importingCountry);
        issueDate = Values.text(issueDate);
        issuedRetroactively = issuedRetroactively != null && issuedRetroactively;
        readability = Values.readability(readability);
        items = items == null ? List.of() : List.copyOf(items);
    }
}
