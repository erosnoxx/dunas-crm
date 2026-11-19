package com.crm.common.application.exceptions;

import lombok.Getter;

import java.util.List;

/**
 * 400 — entrada inválida ou malformada.
 *
 * <p>É também o destino da tradução de {@code ValidationException} (VO inválido) feita no
 * aspect de {@code @UseCase}, e o tipo em que a web converte as violações do Bean Validation.
 */
@Getter
public class BadRequestException extends ApplicationException {
    private final List<InvalidParam> invalidParams;

    public BadRequestException(String message) {
        super(message);
        this.invalidParams = List.of();
    }

    public BadRequestException(String message, Throwable cause) {
        super(message, cause);
        this.invalidParams = List.of();
    }

    public BadRequestException(String message, List<InvalidParam> invalidParams) {
        super(message);
        this.invalidParams = List.copyOf(invalidParams);
    }

    public BadRequestException(String code, String message) {
        super(code, message, null);
        this.invalidParams = List.of();
    }

    public BadRequestException(
            String code,
            String message,
            Throwable cause
    ) {
        super(code, message, cause);
        this.invalidParams = List.of();
    }
}
