package com.crm.common.application.exceptions;

/** 409 — conflito de estado: duplicidade, já usado ou expirado, modificação concorrente. */
public class ConflictException extends ApplicationException {
    public ConflictException(String message) {
        super(message);
    }

    public ConflictException(String message, Throwable cause) {
        super(message, cause);
    }

    public ConflictException(String code, String message) {
        super(code, message, null);
    }

    public ConflictException(
            String code,
            String message,
            Throwable cause
    ) {
        super(code, message, cause);
    }
}
