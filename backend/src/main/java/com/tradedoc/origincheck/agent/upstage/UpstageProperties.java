package com.tradedoc.origincheck.agent.upstage;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * app.upstage.* — Upstage Agents API (OpenAI 호환 /v2).
 *
 * @param baseUrl   https://api.upstage.ai/v2
 * @param apiKey    UPSTAGE_API_KEY. 코드·저장소에 적지 않는다
 * @param projectId x-project-id 헤더
 * @param agentId   Studio 워크플로(agent) ID. responses.create 의 model
 * @param configId  워크플로 설정 번호. responses.create 의 config_id
 */
@ConfigurationProperties(prefix = "app.upstage")
public record UpstageProperties(String baseUrl, String apiKey, String projectId, String agentId, String configId) {
}
