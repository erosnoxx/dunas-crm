package com.crm.common.application.exceptions;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ConcurrentUpdateException")
class ConcurrentUpdateExceptionTest {

    @Test
    @DisplayName("é conflito (409) com o código concurrent_update e a mensagem de tentar de novo")
    void isConflictWithCode() {
        var exception = new ConcurrentUpdateException();

        assertInstanceOf(ConflictException.class, exception);
        assertEquals(Optional.of("concurrent_update"), exception.getCode());
        assertEquals("o registro foi alterado ao mesmo tempo por outra operação; tente de novo", exception.getMessage());
    }

    @Test
    @DisplayName("guarda a causa vinda da persistência")
    void keepsCause() {
        var cause = new IllegalStateException("versão velha");

        assertSame(cause, new ConcurrentUpdateException(cause).getCause());
    }
}
