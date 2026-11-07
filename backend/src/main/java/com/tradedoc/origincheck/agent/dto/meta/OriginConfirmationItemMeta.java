package com.tradedoc.origincheck.agent.dto.meta;

import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

/**
 * OriginConfirmationItem 의 필드별 신뢰도·위치. 필드 이름은 OriginConfirmationItem 과 같고, 값이 없으면 null.
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record OriginConfirmationItemMeta(
        FieldMeta agreement,
        FieldMeta hsCode,
        FieldMeta description,
        FieldMeta originCriterion,
        FieldMeta fulfilled,
        FieldMeta originCountry,
        FieldMeta periodFrom,
        FieldMeta periodTo
) {
}
