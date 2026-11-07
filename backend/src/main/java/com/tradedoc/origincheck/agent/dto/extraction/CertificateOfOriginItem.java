package com.tradedoc.origincheck.agent.dto.extraction;

import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

/**
 * CertificateOfOrigin 의 품목 한 줄.
 * <p>문서에 없는 문자열은 "", 숫자는 0, 참거짓은 false 로 온다(워크플로 규칙). null 이나 빠진 키도 같은 규칙으로 바꾼다.
 *
 * @param description 7번 칸의 품명(포장 수·수량 문구는 뺀다). 예: 'COTTON T-SHIRTS'.
 * @param quantity 7번 칸 괄호 안 수량. 숫자만.
 * @param hsCode 숫자만, 보통 6자리. 예: 610910
 * @param originCriterion 예: WO, CTH, CTC, RVC 40%
 * @param grossWeightKg 9번 칸 총중량 KGS. 숫자만.
 * @param fobValue 9번 칸 FOB 금액(USD). 쉼표 빼고 숫자만.
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record CertificateOfOriginItem(
        String description,
        Double quantity,
        String hsCode,
        String originCriterion,
        Double grossWeightKg,
        Double fobValue
) {
    public CertificateOfOriginItem {
        description = Values.text(description);
        quantity = quantity == null ? 0.0 : quantity;
        hsCode = Values.text(hsCode);
        originCriterion = Values.text(originCriterion);
        grossWeightKg = grossWeightKg == null ? 0.0 : grossWeightKg;
        fobValue = fobValue == null ? 0.0 : fobValue;
    }
}
