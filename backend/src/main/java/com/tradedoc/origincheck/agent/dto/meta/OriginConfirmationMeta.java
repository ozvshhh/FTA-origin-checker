package com.tradedoc.origincheck.agent.dto.meta;

import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

import java.util.List;
import java.util.Map;

/**
 * OriginConfirmationExtraction 의 필드별 신뢰도·위치. 필드 이름은 OriginConfirmationExtraction 과 같고, 값이 없으면 null.
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record OriginConfirmationMeta(
        FieldMeta issueNo,
        FieldMeta supplier,
        FieldMeta recipient,
        FieldMeta issueDate,
        FieldMeta readability,
        List<OriginConfirmationItemMeta> items,
        List<Map<String, Object>> sourceFiles
) {
    public OriginConfirmationMeta {
        items = items == null ? List.of() : List.copyOf(items);
        sourceFiles = sourceFiles == null ? List.of() : List.copyOf(sourceFiles);
    }
}
