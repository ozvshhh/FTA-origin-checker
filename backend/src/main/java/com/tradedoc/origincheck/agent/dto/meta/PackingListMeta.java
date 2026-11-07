package com.tradedoc.origincheck.agent.dto.meta;

import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

import java.util.List;
import java.util.Map;

/**
 * PackingListExtraction 의 필드별 신뢰도·위치. 필드 이름은 PackingListExtraction 과 같고, 값이 없으면 null.
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record PackingListMeta(
        FieldMeta invoiceNo,
        FieldMeta consignee,
        FieldMeta goodsDescription,
        FieldMeta quantity,
        FieldMeta cartons,
        FieldMeta netWeightKg,
        FieldMeta grossWeightKg,
        FieldMeta readability,
        List<Map<String, Object>> sourceFiles
) {
    public PackingListMeta {
        sourceFiles = sourceFiles == null ? List.of() : List.copyOf(sourceFiles);
    }
}
