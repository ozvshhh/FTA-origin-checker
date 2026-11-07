package com.tradedoc.origincheck.agent;

/** agent 호출 실패(네트워크·인증·4xx/5xx). */
public class AgentException extends RuntimeException {

    public AgentException(String message, Throwable cause) {
        super(message, cause);
    }

    public AgentException(String message) {
        super(message);
    }
}
