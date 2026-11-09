package com.tradedoc.origincheck.agent.dto.extraction;

import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

/**
 * Invoice 의 품목 한 줄.
 * <p>문서에 없는 문자열은 "", 숫자는 0, 참거짓은 false 로 온다(워크플로 규칙). null 이나 빠진 키도 같은 규칙으로 바꾼다.
 *
 * @param description 품명 표기 그대로.
 * @param hsCode 숫자만, 보통 10자리. 예: 6109100000
 * @param quantity 수량. 쉼표·단위 빼고 숫자만.
 * @param amount 금액(USD). 쉼표 빼고 숫자만.
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record InvoiceItem(
        String description,
        String hsCode,
        Double quantity,
        Double amount
) {
    public InvoiceItem {
        description = Values.text(description);
        hsCode = Values.text(hsCode);
        quantity = quantity == null ? 0.0 : quantity;
        amount = amount == null ? 0.0 : amount;
    }
}
