package com.tradedoc.origincheck.agent;

/**
 * agent 제출 결과.
 *
 * @param jobId  agent 작업 ID (결과 조회에 쓴다)
 * @param fileId agent 쪽에 올린 파일 ID. 처리가 끝나면 지운다. 올린 파일이 없으면 null
 */
public record AgentSubmission(String jobId, String fileId) {
}
