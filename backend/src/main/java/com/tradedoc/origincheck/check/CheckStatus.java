package com.tradedoc.origincheck.check;

import com.fasterxml.jackson.annotation.JsonValue;
import com.tradedoc.origincheck.common.ApiException;
import com.tradedoc.origincheck.common.ErrorCode;

/**
 * 점검 건 상태. 응답에는 소문자 값으로 나간다.
 * QUEUED(접수, agent 제출 전) → IN_PROGRESS(agent 처리 중) → COMPLETED | FAILED
 */
public enum CheckStatus {
    QUEUED("queued"),
    IN_PROGRESS("in_progress"),
    COMPLETED("completed"),
    FAILED("failed");

    private final String value;

    CheckStatus(String value) {
        this.value = value;
    }

    @JsonValue
    public String value() {
        return value;
    }

    /** 쿼리 파라미터용. 소문자 값(completed) 또는 이름(COMPLETED). 비었으면 null. */
    public static CheckStatus fromValue(String v) {
        if (v == null || v.isBlank()) {
            return null;
        }
        for (CheckStatus s : values()) {
            if (s.value.equalsIgnoreCase(v.trim()) || s.name().equalsIgnoreCase(v.trim())) {
                return s;
            }
        }
        throw new ApiException(ErrorCode.VALIDATION_ERROR, "status 는 queued · in_progress · completed · failed 중 하나입니다.");
    }

    public boolean isFinished() {
        return this == COMPLETED || this == FAILED;
    }
}
