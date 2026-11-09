package com.tradedoc.origincheck.agent;

/**
 * agent 호출 실패(네트워크·인증·4xx/5xx).
 * {@code retryable} 이면 잠시 뒤 다시 하면 될 수 있는 오류(연결 끊김·시간 초과·5xx·429)다.
 */
public class AgentException extends RuntimeException {

    private final boolean retryable;

    public AgentException(String message, Throwable cause, boolean retryable) {
        super(message, cause);
        this.retryable = retryable;
    }

    public AgentException(String message, Throwable cause) {
        this(message, cause, false);
    }

    public AgentException(String message) {
        this(message, null, false);
    }

    /** 다시 시도하면 될 수 있는 일시적 오류 */
    public static AgentException transientError(String message, Throwable cause) {
        return new AgentException(message, cause, true);
    }

    public boolean isRetryable() {
        return retryable;
    }
}
