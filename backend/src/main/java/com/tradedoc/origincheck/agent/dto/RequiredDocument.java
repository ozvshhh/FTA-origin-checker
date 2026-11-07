package com.tradedoc.origincheck.agent.dto;

import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Optional;

/** missing_documents 값. 패킹리스트는 선택 서류라 없다. */
public enum RequiredDocument {
    INVOICE("인보이스", 2),
    CERTIFICATE_OF_ORIGIN("원산지증명서", 2),
    ORIGIN_CONFIRMATION("원산지확인서", 2),
    BILL_OF_LADING("B/L", 3),
    EXPORT_DECLARATION("수출신고필증", 3);

    private final String label;
    private final int stage;

    RequiredDocument(String label, int stage) {
        this.label = label;
        this.stage = stage;
    }

    @JsonValue
    public String label() {
        return label;
    }

    /** 이 서류가 없을 때의 판정 단계. 2=보완 요청, 3=검토 필요. */
    public int stage() {
        return stage;
    }

    public static Optional<RequiredDocument> find(String value) {
        if (value != null) {
            for (RequiredDocument d : values()) {
                if (d.label.equals(value.trim())) {
                    return Optional.of(d);
                }
            }
        }
        return Optional.empty();
    }
}
