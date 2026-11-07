package com.tradedoc.origincheck.check.dto;

import com.tradedoc.origincheck.agent.dto.AgentCheckResult;
import com.tradedoc.origincheck.agent.dto.AgentExtracted;
import com.tradedoc.origincheck.agent.dto.AgentFollowUp;
import com.tradedoc.origincheck.agent.dto.AgentJudge;
import com.tradedoc.origincheck.agent.dto.AgentMismatch;
import com.tradedoc.origincheck.agent.dto.meta.AgentExtractedMeta;
import com.tradedoc.origincheck.check.CheckStatus;
import com.tradedoc.origincheck.check.OriginCheck;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

import java.time.Instant;
import java.util.List;

/**
 * GET /api/v1/origin-checks/{id} 응답. 점검 건 정보 + agent 결과(AgentCheckResult)를 한 번에 준다.
 * status 가 completed 가 아니면 judge · follow_up 은 null, 목록은 빈 값이다.
 *
 * @param jobId agent 작업 ID (제출 전이면 null)
 * @param error 실패(failed)거나 결과 일부가 빠졌을 때. 없으면 null
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record OriginCheckResponse(
        Long id,
        String title,
        String fileName,
        CheckStatus status,
        Instant createdAt,
        Instant completedAt,
        String jobId,
        AgentJudge judge,
        List<AgentMismatch> mismatches,
        AgentFollowUp followUp,
        AgentExtracted extracted,
        AgentExtractedMeta extractedMeta,
        CheckError error
) {
    public static OriginCheckResponse of(OriginCheck c, AgentCheckResult r) {
        CheckError error = c.getErrorCode() != null ? new CheckError(c.getErrorCode(), c.getErrorMessage())
                : r != null && r.error() != null ? new CheckError("RESULT_INCOMPLETE", r.error())
                : null;
        return new OriginCheckResponse(c.getId(), c.getTitle(), c.getFileName(), c.getStatus(),
                c.getCreatedAt(), c.getCompletedAt(), c.getAgentJobId(),
                r == null ? null : r.judge(),
                r == null ? List.of() : r.mismatches(),
                r == null ? null : r.followUp(),
                r == null ? AgentExtracted.empty() : r.extracted(),
                r == null ? AgentExtractedMeta.empty() : r.extractedMeta(),
                error);
    }
}
