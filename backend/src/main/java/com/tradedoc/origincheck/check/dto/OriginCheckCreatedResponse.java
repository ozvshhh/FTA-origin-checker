package com.tradedoc.origincheck.check.dto;

import com.tradedoc.origincheck.check.CheckStatus;

/** POST /api/v1/origin-checks 응답(202). 결과는 GET /api/v1/origin-checks/{id} 로 조회한다. */
public record OriginCheckCreatedResponse(Long id, CheckStatus status) {
}
