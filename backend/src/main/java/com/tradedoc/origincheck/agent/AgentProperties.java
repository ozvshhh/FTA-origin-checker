package com.tradedoc.origincheck.agent;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * app.agent.*
 *
 * @param mode           upstage(실제 호출) 또는 fake(저장된 결과 JSON 반환)
 * @param fakeResultPath fake 모드에서 돌려줄 AgentCheckResult JSON 경로
 * @param fakeDelay      fake 모드에서 완료까지 걸리는 시간
 * @param pollIntervalMs 처리 중인 건을 확인하는 간격(ms)
 * @param timeout        제출 후 이 시간이 지나도 안 끝나면 실패 처리
 */
@ConfigurationProperties(prefix = "app.agent")
public record AgentProperties(String mode, String fakeResultPath, Duration fakeDelay, long pollIntervalMs, Duration timeout) {
}
