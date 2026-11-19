package com.crm.common.domain.exceptions;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("DomainException")
class DomainExceptionTest {

    @Test
    @DisplayName("sem código, getCode é vazio")
    void withoutCode() {
        var exception = new DomainException("evento já está cancelado");

        assertEquals(Optional.empty(), exception.getCode());
        assertEquals("evento já está cancelado", exception.getMessage());
    }

    @Test
    @DisplayName("guarda o código e a mensagem")
    void keepsCodeAndMessage() {
        var exception = new DomainException("link_used", "este link já foi usado");

        assertEquals(Optional.of("link_used"), exception.getCode());
        assertEquals("este link já foi usado", exception.getMessage());
    }

    @Test
    @DisplayName("ValidationException também leva código")
    void validationKeepsCode() {
        var exception = new ValidationException("code_invalid", "código inválido ou expirado");

        assertEquals(Optional.of("code_invalid"), exception.getCode());
    }
}
