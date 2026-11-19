package com.crm.common.application.exceptions;

/** 403 — autenticado, mas sem permissão para a ação. */
public class ForbiddenException extends ApplicationException {
    public ForbiddenException(String message) {
        super(message);
    }

    public ForbiddenException(String message, Throwable cause) {
        super(message, cause);
    }

    public ForbiddenException(String code, String message) {
        super(code, message, null);
    }

    public ForbiddenException(
            String code,
            String message,
            Throwable cause
    ) {
        super(code, message, cause);
    }
}
