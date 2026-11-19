package com.crm.common.infrastructure.ratelimit;

import com.crm.common.application.exceptions.ExternalServiceException;
import com.crm.common.application.exceptions.RateLimitExceededException;
import com.crm.common.application.ratelimit.RateLimit;
import com.crm.common.application.ratelimit.RateLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.List;

/**
 * Limite de tentativas em Redis, por janela fixa (ADR-8 da feature 01).
 *
 * <p>O incremento e a expiração acontecem num script só: com dois comandos separados, uma queda
 * entre eles deixaria um contador sem expiração, bloqueando o identificador para sempre.
 */
@Slf4j
@RequiredArgsConstructor
class RedisRateLimiterImpl implements RateLimiter {

    static final String EXCEEDED_CODE = "rate_limited";
    static final String EXCEEDED_MESSAGE = "muitas tentativas; tente de novo em alguns minutos";

    private static final RedisScript<Long> INCREMENT = new DefaultRedisScript<>(
            """
            local count = redis.call('INCR', KEYS[1])
            if count == 1 then
                redis.call('PEXPIRE', KEYS[1], ARGV[1])
            end
            return count
            """,
            Long.class
    );

    /**
     * Só decrementa se a chave ainda existe: numa chave já expirada, um DECR isolado a
     * recriaria com valor -1 e sem TTL, travando o identificador para sempre. Nunca deixa o
     * contador negativo, preservando o TTL restante.
     */
    private static final RedisScript<Long> DECREMENT = new DefaultRedisScript<>(
            """
            if redis.call('EXISTS', KEYS[1]) == 0 then
                return 0
            end
            local count = redis.call('DECR', KEYS[1])
            if count < 0 then
                redis.call('SET', KEYS[1], 0, 'KEEPTTL')
                return 0
            end
            return count
            """,
            Long.class
    );

    private final StringRedisTemplate redisTemplate;
    private final RateLimiterProperties properties;

    @Override
    public void consume(RateLimit limit, String identifier) {
        var count = increment(limit, identifier);

        if (count > limit.maxAttempts())
            throw exceeded();
    }

    @Override
    public void refund(RateLimit limit, String identifier) {
        try {
            redisTemplate.execute(DECREMENT, List.of(key(limit, identifier)));
        }

        catch (DataAccessException e) {
            log.warn("falha ao devolver tentativa do escopo {}: {}", limit.scope(), e.getMessage());
        }
    }

    @Override
    public void ensureAllowed(RateLimit limit, String identifier) {
        var count = current(limit, identifier);

        if (count >= limit.maxAttempts())
            throw exceeded();
    }

    @Override
    public void recordFailure(RateLimit limit, String identifier) {
        increment(limit, identifier);
    }

    @Override
    public void reset(RateLimit limit, String identifier) {
        try {
            redisTemplate.delete(key(limit, identifier));
        }

        catch (DataAccessException e) {
            log.warn("falha ao zerar o contador do escopo {}: {}", limit.scope(), e.getMessage());
        }
    }

    private long increment(RateLimit limit, String identifier) {
        try {
            var count = redisTemplate.execute(
                    INCREMENT,
                    List.of(key(limit, identifier)),
                    String.valueOf(limit.window().toMillis())
            );

            return count == null ? 0 : count;
        }

        catch (DataAccessException e) {
            return unavailable(limit, e);
        }
    }

    private long current(RateLimit limit, String identifier) {
        try {
            var raw = redisTemplate.opsForValue().get(key(limit, identifier));

            return raw == null ? 0 : Long.parseLong(raw);
        }

        catch (DataAccessException e) {
            return unavailable(limit, e);
        }
    }

    private long unavailable(RateLimit limit, DataAccessException cause) {
        if (limit.onUnavailable() instanceof RateLimit.Deny deny) {
            log.error("contador de tentativas indisponível no escopo {}; tentativa recusada: {}", limit.scope(), cause.getMessage());

            throw new ExternalServiceException(deny.code(), deny.message(), cause);
        }

        log.warn("contador de tentativas indisponível no escopo {}; tentativa liberada: {}", limit.scope(), cause.getMessage());

        return 0;
    }

    private String key(RateLimit limit, String identifier) {
        return properties.keyPrefix() + limit.scope() + ":" + identifier;
    }

    private static RateLimitExceededException exceeded() {
        return new RateLimitExceededException(EXCEEDED_CODE, EXCEEDED_MESSAGE);
    }
}
