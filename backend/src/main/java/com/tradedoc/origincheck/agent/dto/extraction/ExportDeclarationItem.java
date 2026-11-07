package com.tradedoc.origincheck.agent.dto.extraction;

import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

/**
 * ExportDeclaration 의 품목 한 줄.
 * <p>문서에 없는 문자열은 "", 숫자는 0, 참거짓은 false 로 온다(워크플로 규칙). null 이나 빠진 키도 같은 규칙으로 바꾼다.
 *
 * @param hsCode 세번부호, 숫자만 10자리
 * @param description 거래품명.
 * @param quantity 수량. 쉼표·단위 빼고 숫자만.
 * @param originCountry 원산지. 예: 'KR'.
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record ExportDeclarationItem(
        String hsCode,
        String description,
        Double quantity,
        String originCountry
) {
    public ExportDeclarationItem {
        hsCode = Values.text(hsCode);
        description = Values.text(description);
        quantity = quantity == null ? 0.0 : quantity;
        originCountry = Values.text(originCountry);
    }
}
