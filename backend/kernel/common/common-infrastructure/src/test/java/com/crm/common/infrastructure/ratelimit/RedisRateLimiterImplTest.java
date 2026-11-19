package com.crm.common.infrastructure.ratelimit;

import com.crm.common.application.exceptions.ExternalServiceException;
import com.crm.common.application.exceptions.RateLimitExceededException;
import com.crm.common.application.ratelimit.RateLimit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("RedisRateLimiterImpl")
class RedisRateLimiterImplTest {

    private static final RateLimit LINK = RateLimit.failOpen("email-link", 3, Duration.ofMinutes(15));

    private static final RateLimit CODE = RateLimit.failClosed(
            "email-code",
            5,
            Duration.ofMinutes(15),
            "code_unavailable",
            "não foi possível conferir o código agora; use o link do e-mail"
    );

    private static final String IDENTIFIER = "marina@email.com";
    private static final String LINK_KEY = "crm:ratelimit:email-link:marina@email.com";

    private StringRedisTemplate redisTemplate;
    private ValueOperations<String, String> valueOps;
    private RedisRateLimiterImpl rateLimiter;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        redisTemplate = mock(StringRedisTemplate.class);
        valueOps = mock(ValueOperations.class);

        when(redisTemplate.opsForValue()).thenReturn(valueOps);

        rateLimiter = new RedisRateLimiterImpl(redisTemplate, new RateLimiterProperties("crm:ratelimit:"));
    }

    @SuppressWarnings("unchecked")
    private void counterReturns(long count) {
        when(redisTemplate.execute(any(RedisScript.class), anyList(), anyString())).thenReturn(count);
    }

    @SuppressWarnings("unchecked")
    private void counterIsDown() {
        when(redisTemplate.execute(any(RedisScript.class), anyList(), anyString()))
                .thenThrow(new RedisConnectionFailureException("redis fora"));
    }

    @Test
    @DisplayName("consume até o máximo passa")
    void consumeUpToMaxPasses() {
        counterReturns(3);

        assertThatCode(() -> rateLimiter.consume(LINK, IDENTIFIER)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("consume acima do máximo recusa com rate_limited")
    void consumeAboveMaxRejects() {
        counterReturns(4);

        assertThatThrownBy(() -> rateLimiter.consume(LINK, IDENTIFIER))
                .isInstanceOfSatisfying(
                        RateLimitExceededException.class,
                        exception -> assertThat(exception.getCode()).contains("rate_limited")
                )
                .hasMessage("muitas tentativas; tente de novo em alguns minutos");
    }

    @Test
    @DisplayName("consume manda a chave com prefixo e a janela em milissegundos")
    @SuppressWarnings("unchecked")
    void consumeSendsKeyAndWindow() {
        counterReturns(1);

        rateLimiter.consume(LINK, IDENTIFIER);

        verify(redisTemplate).execute(any(RedisScript.class), eq(List.of(LINK_KEY)), eq("900000"));
    }

    @Test
    @DisplayName("ensureAllowed recusa quando o máximo já foi atingido")
    void ensureAllowedRejectsAtMax() {
        when(valueOps.get(LINK_KEY)).thenReturn("3");

        assertThatThrownBy(() -> rateLimiter.ensureAllowed(LINK, IDENTIFIER))
                .isInstanceOf(RateLimitExceededException.class);
    }

    @Test
    @DisplayName("ensureAllowed passa abaixo do máximo e sem contador")
    void ensureAllowedPassesBelowMax() {
        when(valueOps.get(LINK_KEY)).thenReturn("2");

        assertThatCode(() -> rateLimiter.ensureAllowed(LINK, IDENTIFIER)).doesNotThrowAnyException();

        when(valueOps.get(LINK_KEY)).thenReturn(null);

        assertThatCode(() -> rateLimiter.ensureAllowed(LINK, IDENTIFIER)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("recordFailure conta sem recusar, mesmo acima do máximo")
    void recordFailureNeverRejects() {
        counterReturns(10);

        assertThatCode(() -> rateLimiter.recordFailure(LINK, IDENTIFIER)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("refund devolve uma tentativa consumida")
    @SuppressWarnings("unchecked")
    void refundDecrementsCounter() {
        rateLimiter.refund(LINK, IDENTIFIER);

        verify(redisTemplate).execute(any(RedisScript.class), eq(List.of(LINK_KEY)));
    }

    @Test
    @DisplayName("refund com o Redis fora não lança nada")
    @SuppressWarnings("unchecked")
    void refundFailureIsSwallowed() {
        when(redisTemplate.execute(any(RedisScript.class), eq(List.of(LINK_KEY))))
                .thenThrow(new RedisConnectionFailureException("redis fora"));

        assertThatCode(() -> rateLimiter.refund(LINK, IDENTIFIER)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("reset apaga o contador")
    void resetDeletesCounter() {
        rateLimiter.reset(LINK, IDENTIFIER);

        verify(redisTemplate).delete(LINK_KEY);
    }

    @Test
    @DisplayName("reset com o Redis fora não lança nada")
    void resetFailureIsSwallowed() {
        when(redisTemplate.delete(LINK_KEY)).thenThrow(new RedisConnectionFailureException("redis fora"));

        assertThatCode(() -> rateLimiter.reset(LINK, IDENTIFIER)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Redis fora com failOpen libera a tentativa")
    void failOpenAllowsWhenRedisIsDown() {
        counterIsDown();

        assertThatCode(() -> rateLimiter.consume(LINK, IDENTIFIER)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Redis fora com failClosed recusa com o código dado")
    void failClosedRejectsWhenRedisIsDown() {
        counterIsDown();

        assertThatThrownBy(() -> rateLimiter.consume(CODE, IDENTIFIER))
                .isInstanceOfSatisfying(
                        ExternalServiceException.class,
                        exception -> assertThat(exception.getCode()).contains("code_unavailable")
                );
    }

    @Test
    @DisplayName("failClosed também recusa em ensureAllowed")
    void failClosedRejectsOnEnsureAllowed() {
        when(valueOps.get(any())).thenThrow(new RedisConnectionFailureException("redis fora"));

        assertThatThrownBy(() -> rateLimiter.ensureAllowed(CODE, IDENTIFIER))
                .isInstanceOf(ExternalServiceException.class);
    }
}
