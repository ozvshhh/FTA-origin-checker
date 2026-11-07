package com.tradedoc.origincheck.agent.dto.meta;

import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

/**
 * InvoiceItem 의 필드별 신뢰도·위치. 필드 이름은 InvoiceItem 과 같고, 값이 없으면 null.
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record InvoiceItemMeta(
        FieldMeta description,
        FieldMeta hsCode,
        FieldMeta quantity,
        FieldMeta amount
) {
}
