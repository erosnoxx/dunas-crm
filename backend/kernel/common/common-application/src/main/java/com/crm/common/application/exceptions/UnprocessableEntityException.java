package com.crm.common.application.exceptions;

/**
 * 422 — regra de negócio violada.
 *
 * <p>É o destino da tradução de {@code DomainException} feita no aspect de {@code @UseCase}:
 * uma invariante de aggregate que falha chega à web como 422, sem que a web conheça
 * {@code DomainException}.
 */
public class UnprocessableEntityException extends ApplicationException {
    public UnprocessableEntityException(String message) {
        super(message);
    }

    public UnprocessableEntityException(String message, Throwable cause) {
        super(message, cause);
    }

    public UnprocessableEntityException(String code, String message) {
        super(code, message, null);
    }

    public UnprocessableEntityException(
            String code,
            String message,
            Throwable cause
    ) {
        super(code, message, cause);
    }
}
