package com.tradedoc.origincheck.auth.dto;

import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

/**
 * @param accessToken JWT. 이후 요청에 Authorization: Bearer &lt;access_token&gt; 으로 보낸다
 * @param tokenType   항상 bearer
 * @param expiresIn   토큰 유효 시간(초)
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record TokenResponse(String accessToken, String tokenType, long expiresIn) {

    public static TokenResponse bearer(String token, long expiresIn) {
        return new TokenResponse(token, "bearer", expiresIn);
    }
}
