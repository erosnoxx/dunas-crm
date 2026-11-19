package com.crm.common.application.exceptions;

import java.util.Optional;

/**
 * Raiz das exceptions da camada de application.
 *
 * <p>Deliberadamente NÃO estende {@code DomainException}: é o que permite a tradução no
 * aspect de {@code @UseCase} ser inequívoca (o que é de domínio vira application; o que já
 * é application passa reto) e permite a web capturar só tipos de application, sem nunca
 * nomear um tipo de domínio.
 *
 * <p>Pode levar um código de erro estável, que a web põe no {@code ProblemDetail}
 * (architecture.md §7.3).
 *
 * <p>Abstrata de propósito: sempre se lança um subtipo com status HTTP definido.
 */
public abstract class ApplicationException extends RuntimeException {
    private final String code;

    protected ApplicationException(String message) {
        this(null, message, null);
    }

    protected ApplicationException(String message, Throwable cause) {
        this(null, message, cause);
    }

    protected ApplicationException(
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
