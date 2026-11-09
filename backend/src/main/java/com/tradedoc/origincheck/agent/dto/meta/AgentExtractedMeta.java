package com.tradedoc.origincheck.agent.dto.meta;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tradedoc.origincheck.agent.dto.ExtractNode;

import java.util.List;

/**
 * extracted_meta. extracted 와 같은 구조(스키마 이름 → 문서 배열 → 필드)이고 값마다 FieldMeta 가 붙는다.
 * 배열 순서는 extracted 의 같은 키 배열과 같다(n번째 문서 ↔ n번째 메타).
 */
public record AgentExtractedMeta(
        @JsonProperty(ExtractNode.INVOICE) List<InvoiceMeta> invoices,
        @JsonProperty(ExtractNode.PACKING_LIST) List<PackingListMeta> packingLists,
        @JsonProperty(ExtractNode.BILL_OF_LADING) List<BillOfLadingMeta> billsOfLading,
        @JsonProperty(ExtractNode.CERTIFICATE_OF_ORIGIN) List<CertificateOfOriginMeta> certificatesOfOrigin,
        @JsonProperty(ExtractNode.ORIGIN_CONFIRMATION) List<OriginConfirmationMeta> originConfirmations,
        @JsonProperty(ExtractNode.EXPORT_DECLARATION) List<ExportDeclarationMeta> exportDeclarations
) {
    public AgentExtractedMeta {
        invoices = invoices == null ? List.of() : List.copyOf(invoices);
        packingLists = packingLists == null ? List.of() : List.copyOf(packingLists);
        billsOfLading = billsOfLading == null ? List.of() : List.copyOf(billsOfLading);
        certificatesOfOrigin = certificatesOfOrigin == null ? List.of() : List.copyOf(certificatesOfOrigin);
        originConfirmations = originConfirmations == null ? List.of() : List.copyOf(originConfirmations);
        exportDeclarations = exportDeclarations == null ? List.of() : List.copyOf(exportDeclarations);
    }

    public static AgentExtractedMeta empty() {
        return new AgentExtractedMeta(null, null, null, null, null, null);
    }
}
