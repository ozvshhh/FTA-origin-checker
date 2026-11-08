package com.tradedoc.origincheck.agent;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * app.agent.*
 *
 * @param mode               upstage(실제 호출) 또는 fake(저장된 결과 JSON 반환)
 * @param fakeResultPath     fake 모드에서 돌려줄 AgentCheckResult JSON 경로
 * @param fakeDelay          fake 모드에서 완료까지 걸리는 시간
 * @param pollIntervalMs     처리 중인 건을 확인하는 간격(ms)
 * @param timeout            제출 후 이 시간이 지나도 안 끝나면 실패 처리
 * @param submitMaxAttempts  일시적 오류(네트워크·5xx·429)일 때 제출을 최대 몇 번까지 해 볼지. 기본 5
 * @param submitRetryDelay   첫 재시도까지 기다리는 시간. 시도할 때마다 두 배로 늘린다. 기본 10초
 */
@ConfigurationProperties(prefix = "app.agent")
public record AgentProperties(String mode, String fakeResultPath, Duration fakeDelay, long pollIntervalMs, Duration timeout,
                              Integer submitMaxAttempts, Duration submitRetryDelay) {

    public AgentProperties {
        if (submitMaxAttempts == null || submitMaxAttempts < 1) {
            submitMaxAttempts = 5;
        }
        if (submitRetryDelay == null) {
            submitRetryDelay = Duration.ofSeconds(10);
        }
    }

    /** n 번째 실패(1부터) 뒤 다음 시도까지 기다릴 시간: delay, 2·delay, 4·delay … (최대 10분) */
    public Duration submitBackoff(int failures) {
        Duration d = submitRetryDelay.multipliedBy(1L << Math.min(Math.max(failures - 1, 0), 16));
        return d.compareTo(Duration.ofMinutes(10)) > 0 ? Duration.ofMinutes(10) : d;
    }
}
