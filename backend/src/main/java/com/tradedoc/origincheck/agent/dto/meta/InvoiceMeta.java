package com.tradedoc.origincheck.agent.dto.meta;

import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

import java.util.List;
import java.util.Map;

/**
 * InvoiceExtraction 의 필드별 신뢰도·위치. 필드 이름은 InvoiceExtraction 과 같고, 값이 없으면 null.
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record InvoiceMeta(
        FieldMeta invoiceNo,
        FieldMeta invoiceDate,
        FieldMeta shipper,
        FieldMeta consignee,
        FieldMeta portOfLoading,
        FieldMeta portOfDischarge,
        FieldMeta vessel,
        FieldMeta incoterm,
        FieldMeta totalAmount,
        FieldMeta originDeclaration,
        FieldMeta originDeclarationAuthNo,
        FieldMeta readability,
        List<InvoiceItemMeta> items,
        List<Map<String, Object>> sourceFiles
) {
    public InvoiceMeta {
        items = items == null ? List.of() : List.copyOf(items);
        sourceFiles = sourceFiles == null ? List.of() : List.copyOf(sourceFiles);
    }
}
