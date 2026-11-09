package com.tradedoc.origincheck.check.dto;

/**
 * 점검 실패·경고 정보.
 *
 * @param code    AGENT_ERROR(agent 호출 실패) · AGENT_FAILED(agent 가 실패로 끝남) · AGENT_TIMEOUT(시간 초과) ·
 *                RESULT_PARSE_ERROR(결과 형식 해석 실패) · RESULT_INCOMPLETE(완료됐지만 결과 일부 누락)
 * @param message 사람이 읽는 설명
 */
public record CheckError(String code, String message) {
}
