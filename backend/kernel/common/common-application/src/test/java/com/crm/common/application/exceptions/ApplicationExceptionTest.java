package com.crm.common.application.exceptions;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

@DisplayName("ApplicationException")
class ApplicationExceptionTest {

    @Test
    @DisplayName("sem código, getCode é vazio")
    void withoutCode() {
        assertEquals(Optional.empty(), new NotFoundException("usuário não encontrado").getCode());
    }

    @Test
    @DisplayName("todo subtipo aceita código e mensagem")
    void everySubtypeAcceptsCode() {
        List<ApplicationException> all = List.of(
                new BadRequestException("some_code", "mensagem"),
                new ConflictException("some_code", "mensagem"),
                new ExternalServiceException("some_code", "mensagem"),
                new ForbiddenException("some_code", "mensagem"),
                new InternalServerErrorException("some_code", "mensagem"),
                new NotFoundException("some_code", "mensagem"),
                new RateLimitExceededException("some_code", "mensagem"),
                new UnauthorizedException("some_code", "mensagem"),
                new UnprocessableEntityException("some_code", "mensagem")
        );

        all.forEach(exception -> assertEquals(Optional.of("some_code"), exception.getCode()));
        all.forEach(exception -> assertEquals("mensagem", exception.getMessage()));
    }

    @Test
    @DisplayName("guarda a causa junto do código")
    void keepsCause() {
        var cause = new IllegalStateException("causa");

        var exception = new UnprocessableEntityException("link_used", "este link já foi usado", cause);

        assertSame(cause, exception.getCause());
        assertEquals(Optional.of("link_used"), exception.getCode());
    }

    @Test
    @DisplayName("BadRequestException com código não tem campos inválidos")
    void badRequestWithCodeHasNoInvalidParams() {
        assertEquals(List.of(), new BadRequestException("some_code", "mensagem").getInvalidParams());
    }
}
