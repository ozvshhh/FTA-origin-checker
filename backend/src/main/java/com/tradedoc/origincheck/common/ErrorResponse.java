package com.tradedoc.origincheck.common;

import java.time.Instant;

/**
 * 모든 오류 응답의 본문.
 *
 * @param code      INVALID_FILE, UNSUPPORTED_MEDIA_TYPE, UNAUTHORIZED 등 ({@link ErrorCode})
 * @param message   사람이 읽는 오류 설명
 * @param path      요청 경로
 * @param timestamp 발생 시각 (ISO-8601, UTC)
 */
public record ErrorResponse(String code, String message, String path, Instant timestamp) {

    public static ErrorResponse of(ErrorCode code, String message, String path) {
        return new ErrorResponse(code.name(), message, path, Instant.now());
    }
}
