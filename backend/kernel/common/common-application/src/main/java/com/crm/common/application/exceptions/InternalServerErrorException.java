package com.crm.common.application.exceptions;

/** 500 — falha de mecanismo interno que não é regra de negócio nem integração externa. */
public class InternalServerErrorException extends ApplicationException {
    public InternalServerErrorException(String message) {
        super(message);
    }

    public InternalServerErrorException(String message, Throwable cause) {
        super(message, cause);
    }

    public InternalServerErrorException(String code, String message) {
        super(code, message, null);
    }

    public InternalServerErrorException(
            String code,
            String message,
            Throwable cause
    ) {
        super(code, message, cause);
    }
}
