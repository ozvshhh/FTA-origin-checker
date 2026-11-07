package com.tradedoc.origincheck.agent.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tradedoc.origincheck.agent.dto.extraction.BillOfLadingExtraction;
import com.tradedoc.origincheck.agent.dto.extraction.CertificateOfOriginExtraction;
import com.tradedoc.origincheck.agent.dto.extraction.ExportDeclarationExtraction;
import com.tradedoc.origincheck.agent.dto.extraction.InvoiceExtraction;
import com.tradedoc.origincheck.agent.dto.extraction.OriginConfirmationExtraction;
import com.tradedoc.origincheck.agent.dto.extraction.PackingListExtraction;

import java.util.List;

/**
 * 6종 서류 추출 결과. 키는 워크플로 추출 노드 이름(한글) 그대로다.
 * 같은 종류 서류가 2건이면 원소가 2개(DUPLICATE_DOCUMENT), 없으면 빈 배열.
 */
public record AgentExtracted(
        @JsonProperty(ExtractNode.INVOICE) List<InvoiceExtraction> invoices,
        @JsonProperty(ExtractNode.PACKING_LIST) List<PackingListExtraction> packingLists,
        @JsonProperty(ExtractNode.BILL_OF_LADING) List<BillOfLadingExtraction> billsOfLading,
        @JsonProperty(ExtractNode.CERTIFICATE_OF_ORIGIN) List<CertificateOfOriginExtraction> certificatesOfOrigin,
        @JsonProperty(ExtractNode.ORIGIN_CONFIRMATION) List<OriginConfirmationExtraction> originConfirmations,
        @JsonProperty(ExtractNode.EXPORT_DECLARATION) List<ExportDeclarationExtraction> exportDeclarations
) {
    public AgentExtracted {
        invoices = invoices == null ? List.of() : List.copyOf(invoices);
        packingLists = packingLists == null ? List.of() : List.copyOf(packingLists);
        billsOfLading = billsOfLading == null ? List.of() : List.copyOf(billsOfLading);
        certificatesOfOrigin = certificatesOfOrigin == null ? List.of() : List.copyOf(certificatesOfOrigin);
        originConfirmations = originConfirmations == null ? List.of() : List.copyOf(originConfirmations);
        exportDeclarations = exportDeclarations == null ? List.of() : List.copyOf(exportDeclarations);
    }

    public static AgentExtracted empty() {
        return new AgentExtracted(null, null, null, null, null, null);
    }
}
