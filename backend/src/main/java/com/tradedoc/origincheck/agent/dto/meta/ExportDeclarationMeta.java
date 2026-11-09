package com.tradedoc.origincheck.agent.dto.meta;

import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

import java.util.List;
import java.util.Map;

/**
 * ExportDeclarationExtraction 의 필드별 신뢰도·위치. 필드 이름은 ExportDeclarationExtraction 과 같고, 값이 없으면 null.
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record ExportDeclarationMeta(
        FieldMeta declarationNo,
        FieldMeta declarationDate,
        FieldMeta acceptanceDate,
        FieldMeta exporter,
        FieldMeta manufacturer,
        FieldMeta buyer,
        FieldMeta destinationCountry,
        FieldMeta vessel,
        FieldMeta invoiceNo,
        FieldMeta paymentAmount,
        FieldMeta readability,
        List<ExportDeclarationItemMeta> items,
        List<Map<String, Object>> sourceFiles
) {
    public ExportDeclarationMeta {
        items = items == null ? List.of() : List.copyOf(items);
        sourceFiles = sourceFiles == null ? List.of() : List.copyOf(sourceFiles);
    }
}
