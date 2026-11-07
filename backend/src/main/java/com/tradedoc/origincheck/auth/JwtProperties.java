package com.tradedoc.origincheck.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * app.jwt.*
 *
 * @param secret     HS256 서명 키. 32바이트 이상 무작위 문자열. 코드·저장소에 적지 않는다
 * @param expiresMin 토큰 유효 시간(분)
 * @param issuer     iss 클레임
 */
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(String secret, long expiresMin, String issuer) {
}
