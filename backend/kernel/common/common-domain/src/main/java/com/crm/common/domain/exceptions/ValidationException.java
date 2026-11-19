package com.crm.common.domain.exceptions;

/**
 * Valor recusado por uma regra de validação, normalmente na criação de um Value Object.
 */
public class ValidationException extends DomainException {
    public ValidationException(String message) {
        super(message);
    }

    public ValidationException(String code, String message) {
        super(code, message);
    }
}
