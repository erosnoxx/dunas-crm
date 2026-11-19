package com.crm.common.application.exceptions;

/** 404 — recurso solicitado não existe. */
public class NotFoundException extends ApplicationException {
    public NotFoundException(String message) {
        super(message);
    }

    public NotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public NotFoundException(String code, String message) {
        super(code, message, null);
    }

    public NotFoundException(
            String code,
            String message,
            Throwable cause
    ) {
        super(code, message, cause);
    }
}
