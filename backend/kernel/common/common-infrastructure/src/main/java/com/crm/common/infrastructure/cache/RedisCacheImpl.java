package com.crm.common.infrastructure.cache;

import com.crm.common.application.cache.Cache;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Cache em Redis, com payload em JSON.
 *
 * <p>Usa {@link StringRedisTemplate} + Jackson explícito em vez de um serializador automático
 * de objeto: o que vai para o Redis é JSON legível e estável, sem depender de serialização
 * binária do Java nem do nome da classe — o que quebraria toda entrada cacheada num refactor
 * de pacote.
 *
 * <p><b>Fail-open em tudo.</b> Nenhuma operação propaga falha de Redis: o banco é a fonte de
 * verdade e uma indisponibilidade de cache deve degradar performance, nunca disponibilidade.
 */
@Slf4j
@RequiredArgsConstructor
class RedisCacheImpl implements Cache {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final CacheProperties properties;

    @Override
    public <T> Optional<T> get(String key, Class<T> type) {
        try {
            var raw = redisTemplate.opsForValue().get(prefixed(key));

            if (raw == null)
                return Optional.empty();

            return Optional.of(objectMapper.readValue(raw, type));
        }

        catch (Exception e) {
            // inclui payload ilegível (mudança de formato), não só Redis fora do ar: em ambos
            // os casos o certo é tratar como cache miss, não falhar a requisição.
            log.warn("falha ao ler do cache a chave {} — seguindo como miss: {}", key, e.getMessage());

            return Optional.empty();
        }
    }

    @Override
    public <T> T getOrLoad(String key, Class<T> type, Duration ttl, Supplier<T> loader) {
        var cached = get(key, type);

        if (cached.isPresent())
            return cached.get();

        var loaded = loader.get();

        if (loaded == null)
            return null;

        put(key, loaded, ttl);

        return loaded;
    }

    @Override
    public void put(String key, Object value, Duration ttl) {
        try {
            redisTemplate.opsForValue().set(
                    prefixed(key),
                    objectMapper.writeValueAsString(value),
                    effectiveTtl(ttl)
            );
        }

        catch (Exception e) {
            log.warn("falha ao gravar no cache a chave {} — ignorado: {}", key, e.getMessage());
        }
    }

    @Override
    public void evict(String key) {
        try {
            redisTemplate.delete(prefixed(key));
        }

        catch (Exception e) {
            log.warn("falha ao invalidar a chave {} do cache: {}", key, e.getMessage());
        }
    }

    /**
     * SCAN, nunca KEYS: {@code KEYS} varre o keyspace inteiro bloqueando o servidor de cache
     * para todos os clientes. SCAN itera em lotes e não bloqueia.
     */
    @Override
    public void evictByPrefix(String prefix) {
        var pattern = prefixed(prefix) + "*";

        var options = ScanOptions.scanOptions()
                .match(pattern)
                .count(properties.scanBatchSize())
                .build();

        try (Cursor<String> cursor = redisTemplate.scan(options)) {
            var batch = new ArrayList<String>();

            cursor.forEachRemaining(batch::add);

            if (batch.isEmpty())
                return;

            redisTemplate.delete(batch);

            log.debug("{} chave(s) invalidada(s) sob o prefixo {}", batch.size(), prefix);
        }

        catch (Exception e) {
            log.warn("falha ao invalidar o prefixo {} do cache: {}", prefix, e.getMessage());
        }
    }

    private String prefixed(String key) {
        return properties.keyPrefix() + key;
    }

    /** Nunca grava entrada sem expiração: sem TTL, uma invalidação esquecida vira dado eterno. */
    private Duration effectiveTtl(Duration ttl) {
        if (ttl == null || ttl.isZero() || ttl.isNegative())
            return properties.defaultTtl();

        return ttl;
    }
}
