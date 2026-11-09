package com.tradedoc.origincheck.agent.dto;

/**
 * 후속 노드 결과.
 *
 * @param kind 후속 노드 이름
 * @param text 노드 원문(마크다운)
 */
public record AgentFollowUp(FollowUpKind kind, String text) {
}
