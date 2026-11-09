package com.tradedoc.origincheck.agent.dto;

/**
 * mismatch_codes 마다 단계와 화면용 설명.
 *
 * @param code  불일치 코드
 * @param stage 1=미적용, 2=보완 요청, 3=검토 필요. 모르는 코드면 null
 * @param label 화면 표시용 설명. 모르는 코드면 코드 그대로
 */
public record AgentMismatch(String code, Integer stage, String label) {

    public static AgentMismatch of(String code) {
        return MismatchCode.find(code)
                .map(c -> new AgentMismatch(code, c.stage(), c.label()))
                .orElseGet(() -> new AgentMismatch(code, null, code));
    }
}
