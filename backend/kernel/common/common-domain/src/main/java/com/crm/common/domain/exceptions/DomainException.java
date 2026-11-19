package com.crm.common.domain.exceptions;

import java.util.Optional;

/**
 * Regra de domínio violada.
 *
 * <p>Pode levar um código de erro estável ({@code snake_case}, em inglês) quando o frontend
 * precisa distinguir este erro de outro com o mesmo status (architecture.md §7.3). O aspect de
 * {@code @UseCase} preserva o código ao traduzir para a exception de application.
 */
public class DomainException extends RuntimeException {
    private final String code;

    public DomainException(String message) {
        this(null, message, null);
    }

    public DomainException(String message, Throwable cause) {
        this(null, message, cause);
    }

    public DomainException(String code, String message) {
        this(code, message, null);
    }

    protected DomainException(
            String code,
            String message,
            Throwable cause
    ) {
        super(message, cause);
        this.code = code;
    }

    public Optional<String> getCode() {
        return Optional.ofNullable(code);
    }
}