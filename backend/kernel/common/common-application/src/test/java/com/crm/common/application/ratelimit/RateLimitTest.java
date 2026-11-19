package com.crm.common.application.ratelimit;

import com.crm.common.application.exceptions.InternalServerErrorException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("RateLimit")
class RateLimitTest {

    @Test
    @DisplayName("failOpen libera quando o contador não responde")
    void failOpenAllows() {
        var limit = RateLimit.failOpen("login", 5, Duration.ofMinutes(5));

        assertInstanceOf(RateLimit.Allow.class, limit.onUnavailable());
    }

    @Test
    @DisplayName("failClosed guarda o código e a mensagem da recusa")
    void failClosedKeepsCodeAndMessage() {
        var limit = RateLimit.failClosed(
                "email-code",
                5,
                Duration.ofMinutes(15),
                "code_unavailable",
                "não foi possível conferir o código agora; use o link do e-mail"
        );

        var deny = assertInstanceOf(RateLimit.Deny.class, limit.onUnavailable());

        assertEquals("code_unavailable", deny.code());
    }

    @Test
    @DisplayName("recusa escopo vazio, máximo não positivo e janela não positiva")
    void rejectsInvalidDefinition() {
        assertThrows(InternalServerErrorException.class, () -> RateLimit.failOpen(" ", 5, Duration.ofMinutes(5)));
        assertThrows(InternalServerErrorException.class, () -> RateLimit.failOpen("login", 0, Duration.ofMinutes(5)));
        assertThrows(InternalServerErrorException.class, () -> RateLimit.failOpen("login", 5, Duration.ZERO));
    }
}
