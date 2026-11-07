package com.tradedoc.origincheck.agent.dto.meta;

import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

import java.util.List;
import java.util.Map;

/**
 * BillOfLadingExtraction 의 필드별 신뢰도·위치. 필드 이름은 BillOfLadingExtraction 과 같고, 값이 없으면 null.
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record BillOfLadingMeta(
        FieldMeta blNo,
        FieldMeta shipper,
        FieldMeta vessel,
        FieldMeta voyageNo,
        FieldMeta portOfLoading,
        FieldMeta portOfDischarge,
        FieldMeta transshipmentPort,
        FieldMeta goodsDescription,
        FieldMeta grossWeightKg,
        FieldMeta ladenOnBoardDate,
        FieldMeta readability,
        List<Map<String, Object>> sourceFiles
) {
    public BillOfLadingMeta {
        sourceFiles = sourceFiles == null ? List.of() : List.copyOf(sourceFiles);
    }
}
