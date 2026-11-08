package com.tradedoc.origincheck.check.dto;

import com.tradedoc.origincheck.agent.dto.Decision;
import com.tradedoc.origincheck.check.CheckStatus;
import com.tradedoc.origincheck.check.OriginCheck;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

import java.time.Instant;

/**
 * 목록 한 건. decision · invoice_no 는 완료 전에는 null.
 * done_at: 담당자가 완료 처리한 시각. null 이면 진행 중.
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record OriginCheckSummaryResponse(
        Long id,
        String title,
        String fileName,
        CheckStatus status,
        Decision decision,
        String invoiceNo,
        Instant createdAt,
        Instant completedAt,
        Instant doneAt
) {
    public static OriginCheckSummaryResponse from(OriginCheck c) {
        return new OriginCheckSummaryResponse(c.getId(), c.getTitle(), c.getFileName(), c.getStatus(),
                c.getDecision(), c.getInvoiceNo(), c.getCreatedAt(), c.getCompletedAt(), c.getDoneAt());
    }
}
