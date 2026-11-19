package com.crm.common.application.ratelimit;


import com.crm.common.application.exceptions.InternalServerErrorException;

import java.time.Duration;

/**
 * Um limite de tentativas: escopo, máximo por janela e o que fazer quando o contador não
 * responde.
 *
 * <p>{@code common} não conhece o significado do escopo: é uma string de quem chama (ADR-8 da
 * feature 01).
 */
public record RateLimit(
        String scope,
        int maxAttempts,
        Duration window,
        Unavailable onUnavailable
) {
    public RateLimit {
        if (scope == null || scope.isBlank())
            throw new InternalServerErrorException("limite de tentativas sem escopo");

        if (maxAttempts <= 0)
            throw new InternalServerErrorException("limite de tentativas sem máximo positivo: " + scope);

        if (window == null || window.isZero() || window.isNegative())
            throw new InternalServerErrorException("limite de tentativas sem janela positiva: " + scope);

        if (onUnavailable == null)
            throw new InternalServerErrorException("limite de tentativas sem política de indisponibilidade: " + scope);
    }

    /** Contador fora do ar: libera a tentativa e registra alerta. */
    public static RateLimit failOpen(
            String scope,
            int maxAttempts,
            Duration window
    ) {
        return new RateLimit(scope, maxAttempts, window, new Allow());
    }

    /** Contador fora do ar: recusa a tentativa com o código e a mensagem dados. */
    public static RateLimit failClosed(
            String scope,
            int maxAttempts,
            Duration window,
            String code,
            String message
    ) {
        return new RateLimit(scope, maxAttempts, window, new Deny(code, message));
    }

    /** O que acontece quando o contador não responde. */
    public sealed interface Unavailable permits Allow, Deny {
    }

    public record Allow() implements Unavailable {
    }

    public record Deny(String code, String message) implements Unavailable {
    }
}
