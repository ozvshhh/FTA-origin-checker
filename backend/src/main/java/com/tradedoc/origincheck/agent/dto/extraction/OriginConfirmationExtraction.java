package com.tradedoc.origincheck.agent.dto.extraction;

import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

import java.util.List;

/**
 * 원산지확인서_추출 (국내 공급사 → 수출자).
 * <p>문서에 없는 문자열은 "", 숫자는 0, 참거짓은 false 로 온다(워크플로 규칙). null 이나 빠진 키도 같은 규칙으로 바꾼다.
 *
 * @param issueNo 1. 발급번호. 표기 그대로. 예: 'OC-2026-0001'. 없으면 빈 문자열.
 * @param supplier 2. 공급하는 자의 상호만. 예: '(주)한빛니트'. 없으면 빈 문자열.
 * @param recipient 3. 공급받는 자의 상호만. 없으면 빈 문자열.
 * @param issueDate 끝의 작성일. YYYY-MM-DD. 없으면 빈 문자열.
 * @param readability 핵심 값을 모두 읽었으면 clear, 아니면 unclear. 서류가 없으면 빈 문자열
 * @param items 품목 줄마다 하나
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record OriginConfirmationExtraction(
        String issueNo,
        String supplier,
        String recipient,
        String issueDate,
        String readability,
        List<OriginConfirmationItem> items
) {
    public OriginConfirmationExtraction {
        issueNo = Values.text(issueNo);
        supplier = Values.text(supplier);
        recipient = Values.text(recipient);
        issueDate = Values.text(issueDate);
        readability = Values.readability(readability);
        items = items == null ? List.of() : List.copyOf(items);
    }
}
