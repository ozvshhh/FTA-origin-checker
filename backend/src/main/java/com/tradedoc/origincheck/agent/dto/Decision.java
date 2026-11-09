package com.tradedoc.origincheck.agent.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/** 원산지_판정 decision. 값(label)은 워크플로 표기(한글) 그대로 주고받는다. stage 가 작을수록 우선한다. */
public enum Decision {
    NOT_APPLICABLE("미적용", 1),
    SUPPLEMENT_REQUEST("보완 요청", 2),
    REVIEW_REQUIRED("검토 필요", 3),
    PREFERENTIAL("특혜 적용", 4);

    private final String label;
    private final int stage;

    Decision(String label, int stage) {
        this.label = label;
        this.stage = stage;
    }

    @JsonValue
    public String label() {
        return label;
    }

    public int stage() {
        return stage;
    }

    /** 모르는 값이 오면 사람이 봐야 하므로 '검토 필요'로 둔다. */
    @JsonCreator
    public static Decision fromLabel(String value) {
        if (value != null) {
            String v = value.trim();
            for (Decision d : values()) {
                if (d.label.equals(v) || d.name().equals(v)) {
                    return d;
                }
            }
        }
        return REVIEW_REQUIRED;
    }

    public static boolean isKnown(String value) {
        if (value == null) {
            return false;
        }
        for (Decision d : values()) {
            if (d.label.equals(value.trim())) {
                return true;
            }
        }
        return false;
    }
}
