package com.crm.common.application.ratelimit;

/**
 * Limite de tentativas por escopo e identificador (e-mail, IP, id do token).
 *
 * <p>Ao exceder, lança {@code RateLimitExceededException} com o código {@code rate_limited}.
 * Quando o contador não responde, segue a política do {@link RateLimit}: libera ou recusa.
 */
public interface RateLimiter {

    /** Conta esta tentativa e recusa se ela passou do máximo. Toda chamada conta. */
    void consume(RateLimit limit, String identifier);

    /**
     * Devolve uma tentativa contada por {@link #consume}, para quem tenta com sucesso não
     * gastar o limite de quem errou antes. Nunca deixa o contador negativo. Falha ao devolver
     * nunca bloqueia ninguém: só vai para o log, como {@link #reset}.
     *
     * <p>Este é o padrão seguro para "só falhas contam" quando a mesma pessoa pode tentar em
     * paralelo: {@link #ensureAllowed} seguido de {@link #recordFailure} deixa uma janela onde
     * tentativas concorrentes passam todas antes de qualquer uma contar, porque a conferência e a
     * contagem são chamadas separadas. {@code consume} seguido de {@code refund} fecha essa
     * janela, porque o incremento do Redis serializa as tentativas concorrentes antes de
     * qualquer verificação de negócio acontecer.
     */
    void refund(RateLimit limit, String identifier);

    /**
     * Recusa se o máximo já foi atingido, sem contar a tentativa.
     *
     * @deprecated seguido de {@link #recordFailure}, tem uma corrida entre a conferência e a
     *     contagem da falha quando a mesma pessoa tenta em paralelo. Prefira {@link #consume}
     *     seguido de {@link #refund} no acerto.
     */
    @Deprecated
    void ensureAllowed(RateLimit limit, String identifier);

    /**
     * Conta uma falha, sem recusar.
     *
     * @deprecated ver {@link #ensureAllowed}. Prefira {@link #consume} seguido de
     *     {@link #refund} no acerto.
     */
    @Deprecated
    void recordFailure(RateLimit limit, String identifier);

    /** Zera o contador. Falha ao zerar nunca bloqueia ninguém: só vai para o log. */
    void reset(RateLimit limit, String identifier);
}
