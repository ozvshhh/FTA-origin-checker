package com.tradedoc.origincheck.agent.dto;

import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

import java.util.List;

/**
 * 원산지_판정 노드 결과. 채점 정답지(answer.json)의 answer 와 같은 5개 키.
 *
 * @param invoiceNo        인보이스 송장번호. 인보이스가 없으면 수출신고필증 송품장부호. 못 읽으면 null
 * @param decision         판정
 * @param reason           1~2문장, 실제 값 포함
 * @param mismatchCodes    불일치 코드(MismatchCode). 없으면 []. 모르는 코드도 그대로 둔다
 * @param missingDocuments 빠진 서류(RequiredDocument 표기). 없으면 []
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record AgentJudge(
        String invoiceNo,
        Decision decision,
        String reason,
        List<String> mismatchCodes,
        List<String> missingDocuments
) {
    public AgentJudge {
        decision = decision == null ? Decision.REVIEW_REQUIRED : decision;
        reason = reason == null ? "" : reason;
        mismatchCodes = mismatchCodes == null ? List.of() : List.copyOf(mismatchCodes);
        missingDocuments = missingDocuments == null ? List.of() : List.copyOf(missingDocuments);
    }

    public AgentJudge withDecision(Decision newDecision) {
        return new AgentJudge(invoiceNo, newDecision, reason, mismatchCodes, missingDocuments);
    }
}
