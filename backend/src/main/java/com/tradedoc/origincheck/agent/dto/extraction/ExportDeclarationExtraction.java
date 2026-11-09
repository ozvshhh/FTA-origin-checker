package com.tradedoc.origincheck.agent.dto.extraction;

import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

import java.util.List;

/**
 * 수출신고필증_추출.
 * <p>문서에 없는 문자열은 "", 숫자는 0, 참거짓은 false 로 온다(워크플로 규칙). null 이나 빠진 키도 같은 규칙으로 바꾼다.
 *
 * @param declarationNo 신고번호 표기 그대로. 없으면 빈 문자열.
 * @param declarationDate 신고일자. YYYY-MM-DD로 정규화. 예: 2026/09/09 → 2026-09-09. 없으면 빈 문자열.
 * @param acceptanceDate 신고수리일자. YYYY-MM-DD. 신고일자가 아니다. 없으면 빈 문자열.
 * @param exporter 수출대행자·수출화주 상호(괄호 안 사업자번호는 뺀다). 없으면 빈 문자열.
 * @param manufacturer 제조자 상호. 없으면 빈 문자열.
 * @param buyer 구매자 상호. 없으면 빈 문자열.
 * @param destinationCountry 목적국. 예: 'VIETNAM'. 없으면 빈 문자열.
 * @param vessel 선박명. 없으면 빈 문자열.
 * @param invoiceNo 송품장부호
 * @param paymentAmount 표기 그대로. 예: FOB-USD-35,100.00
 * @param readability 핵심 값을 모두 읽었으면 clear, 아니면 unclear. 서류가 없으면 빈 문자열
 * @param items 품목 줄마다 하나
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record ExportDeclarationExtraction(
        String declarationNo,
        String declarationDate,
        String acceptanceDate,
        String exporter,
        String manufacturer,
        String buyer,
        String destinationCountry,
        String vessel,
        String invoiceNo,
        String paymentAmount,
        String readability,
        List<ExportDeclarationItem> items
) {
    public ExportDeclarationExtraction {
        declarationNo = Values.text(declarationNo);
        declarationDate = Values.text(declarationDate);
        acceptanceDate = Values.text(acceptanceDate);
        exporter = Values.text(exporter);
        manufacturer = Values.text(manufacturer);
        buyer = Values.text(buyer);
        destinationCountry = Values.text(destinationCountry);
        vessel = Values.text(vessel);
        invoiceNo = Values.text(invoiceNo);
        paymentAmount = Values.text(paymentAmount);
        readability = Values.readability(readability);
        items = items == null ? List.of() : List.copyOf(items);
    }
}
