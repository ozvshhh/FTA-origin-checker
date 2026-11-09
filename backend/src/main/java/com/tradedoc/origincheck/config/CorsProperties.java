package com.tradedoc.origincheck.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/** app.cors.allowed-origins — 쉼표로 여러 개. 예: http://localhost:5173 */
@ConfigurationProperties(prefix = "app.cors")
public record CorsProperties(List<String> allowedOrigins) {
}
