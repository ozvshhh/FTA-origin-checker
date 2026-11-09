package com.tradedoc.origincheck.agent.dto.extraction;

import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

/**
 * OriginConfirmation 의 품목 한 줄.
 * <p>문서에 없는 문자열은 "", 숫자는 0, 참거짓은 false 로 온다(워크플로 규칙). null 이나 빠진 키도 같은 규칙으로 바꾼다.
 *
 * @param agreement 협정명칭. 예: 한-아세안 FTA
 * @param hsCode 숫자만, 적힌 자릿수 그대로(테스트 데이터는 6자리)
 * @param description 7. 품명·규격.
 * @param originCriterion 예: WO, CTH, CTSH, BD
 * @param fulfilled 충족 여부. Y · N · 빈 문자열
 * @param originCountry 11. 원산지. 예: 'KR'.
 * @param periodFrom 포괄확인기간 시작일. 단수 확인서면 빈 문자열
 * @param periodTo 12. 포괄확인기간 종료일. YYYY-MM-DD. 없으면 빈 문자열.
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record OriginConfirmationItem(
        String agreement,
        String hsCode,
        String description,
        String originCriterion,
        String fulfilled,
        String originCountry,
        String periodFrom,
        String periodTo
) {
    public OriginConfirmationItem {
        agreement = Values.text(agreement);
        hsCode = Values.text(hsCode);
        description = Values.text(description);
        originCriterion = Values.text(originCriterion);
        fulfilled = Values.fulfilled(fulfilled);
        originCountry = Values.text(originCountry);
        periodFrom = Values.text(periodFrom);
        periodTo = Values.text(periodTo);
    }
}
