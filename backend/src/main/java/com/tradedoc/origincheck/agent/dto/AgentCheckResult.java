package com.tradedoc.origincheck.agent.dto;

import com.tradedoc.origincheck.agent.dto.meta.AgentExtractedMeta;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

import java.util.List;

/**
 * AI agent(Upstage 워크플로) 점검 결과 전체. DB 의 origin_checks.result_json 에 이 모양 그대로 저장한다.
 *
 * @param jobId         Upstage 작업 ID
 * @param judge         원산지_판정 결과. 판정 노드 출력이 없으면 null
 * @param mismatches    mismatch_codes 마다 단계와 화면용 설명
 * @param followUp      후속 노드 결과. 후속 노드가 없는 판정(특혜 적용)이면 null
 * @param extracted     6종 서류 추출 결과
 * @param extractedMeta extracted 의 값마다 신뢰도(confidence·score)와 문서상 위치(page·boxes)
 * @param error         결과를 만들다 생긴 문제(예: 판정 노드 출력을 찾지 못함). 없으면 null
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record AgentCheckResult(
        String jobId,
        AgentJudge judge,
        List<AgentMismatch> mismatches,
        AgentFollowUp followUp,
        AgentExtracted extracted,
        AgentExtractedMeta extractedMeta,
        String error
) {
    public AgentCheckResult {
        mismatches = mismatches == null ? List.of() : List.copyOf(mismatches);
        extracted = extracted == null ? AgentExtracted.empty() : extracted;
        extractedMeta = extractedMeta == null ? AgentExtractedMeta.empty() : extractedMeta;
    }
}
