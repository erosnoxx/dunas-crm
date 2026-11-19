package com.crm.common.application.exceptions;

/** 503 — falha de integração com sistema externo. */
public class ExternalServiceException extends ApplicationException {
    public ExternalServiceException(String message) {
        super(message);
    }

    public ExternalServiceException(String message, Throwable cause) {
        super(message, cause);
    }

    public ExternalServiceException(String code, String message) {
        super(code, message, null);
    }

    public ExternalServiceException(
            String code,
            String message,
            Throwable cause
    ) {
        super(code, message, cause);
    }
}
