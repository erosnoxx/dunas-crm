package com.crm.common.application.exceptions;

/** 429 — limite de taxa excedido. */
public class RateLimitExceededException extends ApplicationException {
    public RateLimitExceededException(String message) {
        super(message);
    }

    public RateLimitExceededException(String message, Throwable cause) {
        super(message, cause);
    }

    public RateLimitExceededException(String code, String message) {
        super(code, message, null);
    }

    public RateLimitExceededException(
            String code,
            String message,
            Throwable cause
    ) {
        super(code, message, cause);
    }
}
