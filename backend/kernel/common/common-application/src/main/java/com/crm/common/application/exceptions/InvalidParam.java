package com.crm.common.application.exceptions;

/**
 * Um campo inválido, no formato que a web expõe em {@code invalidParams} do ProblemDetail.
 *
 * <p>O payload de erro de validação é o mesmo para Bean Validation e para validação manual —
 * o frontend nunca precisa distinguir a origem do 400.
 */
public record InvalidParam(String name, String reason) {
}
