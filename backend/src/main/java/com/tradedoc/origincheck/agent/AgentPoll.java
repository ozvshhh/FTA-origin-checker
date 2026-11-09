package com.tradedoc.origincheck.agent;

import com.tradedoc.origincheck.agent.dto.AgentCheckResult;

/** agent 작업 조회 결과. */
public record AgentPoll(State state, AgentCheckResult result, String error) {

    public enum State { RUNNING, COMPLETED, FAILED }

    public static AgentPoll running() {
        return new AgentPoll(State.RUNNING, null, null);
    }

    public static AgentPoll completed(AgentCheckResult result) {
        return new AgentPoll(State.COMPLETED, result, null);
    }

    public static AgentPoll failed(String error) {
        return new AgentPoll(State.FAILED, null, error);
    }
}
