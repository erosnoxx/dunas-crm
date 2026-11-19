package com.crm.common.application.exceptions;

/** 401 — sem autenticação válida, ou credencial inválida. */
public class UnauthorizedException extends ApplicationException {
    public UnauthorizedException(String message) {
        super(message);
    }

    public UnauthorizedException(String message, Throwable cause) {
        super(message, cause);
    }

    public UnauthorizedException(String code, String message) {
        super(code, message, null);
    }

    public UnauthorizedException(
            String code,
            String message,
            Throwable cause
    ) {
        super(code, message, cause);
    }
}
