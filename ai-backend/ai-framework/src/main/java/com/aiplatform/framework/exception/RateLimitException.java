package com.aiplatform.framework.exception;

/**
 * 限流异常(429 Too Many Requests)。
 */
public class RateLimitException extends RuntimeException {

    private final int code = 429;
    private final long retryAfterSeconds;

    public RateLimitException(String message, long retryAfterSeconds) {
        super(message);
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public int getCode() {
        return code;
    }

    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
