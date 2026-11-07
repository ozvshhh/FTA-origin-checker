package com.tradedoc.origincheck.agent.dto.extraction;

import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

/**
 * 패킹리스트_추출 (선택 서류).
 * <p>문서에 없는 문자열은 "", 숫자는 0, 참거짓은 false 로 온다(워크플로 규칙). null 이나 빠진 키도 같은 규칙으로 바꾼다.
 *
 * @param invoiceNo 패킹리스트에 적힌 송장번호('No. & Date of Invoice' 칸). 표기 그대로. 없으면 빈 문자열.
 * @param consignee To Applicant / Consignee 회사명만. 주소는 뺀다. 없으면 빈 문자열.
 * @param goodsDescription Goods Description의 품명을 표기 그대로. 없으면 빈 문자열.
 * @param quantity 총 수량. 쉼표 빼고 숫자만. 예: '64,444 PCS' → 64444. 없으면 0.
 * @param cartons 총 포장 수. 'TOTAL: 204 CARTONS' → 204. 없으면 0.
 * @param netWeightKg 총 순중량(Net Weight) kg. TOTAL 줄의 값. 쉼표 빼고 숫자만. 없으면 0.
 * @param grossWeightKg 총 총중량(Gross Weight) kg. TOTAL 줄의 값. 순중량과 바꿔 넣지 않는다. 없으면 0.
 * @param readability 핵심 값을 모두 읽었으면 clear, 아니면 unclear. 서류가 없으면 빈 문자열
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record PackingListExtraction(
        String invoiceNo,
        String consignee,
        String goodsDescription,
        Double quantity,
        Double cartons,
        Double netWeightKg,
        Double grossWeightKg,
        String readability
) {
    public PackingListExtraction {
        invoiceNo = Values.text(invoiceNo);
        consignee = Values.text(consignee);
        goodsDescription = Values.text(goodsDescription);
        quantity = quantity == null ? 0.0 : quantity;
        cartons = cartons == null ? 0.0 : cartons;
        netWeightKg = netWeightKg == null ? 0.0 : netWeightKg;
        grossWeightKg = grossWeightKg == null ? 0.0 : grossWeightKg;
        readability = Values.readability(readability);
    }
}
