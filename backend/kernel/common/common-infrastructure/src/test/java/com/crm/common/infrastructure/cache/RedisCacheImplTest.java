package com.crm.common.infrastructure.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@DisplayName("RedisCacheImpl")
class RedisCacheImplTest {

    private record Dto(String name, int quantity) {
    }

    private static final CacheProperties PROPERTIES =
            new CacheProperties("crm:", Duration.ofMinutes(5), 500);

    private StringRedisTemplate redisTemplate;
    private ValueOperations<String, String> valueOps;
    private RedisCacheImpl cache;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        redisTemplate = mock(StringRedisTemplate.class);
        valueOps = mock(ValueOperations.class);

        when(redisTemplate.opsForValue()).thenReturn(valueOps);

        cache = new RedisCacheImpl(redisTemplate, new ObjectMapper(), PROPERTIES);
    }

    @Test
    @DisplayName("prefixa a chave para isolar o namespace da aplicação")
    void prefixesKeys() {
        cache.get("evento:123", Dto.class);

        verify(valueOps).get("crm:evento:123");
    }

    @Test
    @DisplayName("miss devolve vazio")
    void missReturnsEmpty() {
        when(valueOps.get(any())).thenReturn(null);

        assertThat(cache.get("k", Dto.class)).isEmpty();
    }

    @Test
    @DisplayName("hit desserializa o JSON")
    void hitDeserializes() {
        when(valueOps.get("crm:k")).thenReturn("{\"name\":\"lote 1\",\"quantity\":3}");

        assertThat(cache.get("k", Dto.class)).contains(new Dto("lote 1", 3));
    }

    /**
     * O banco é a fonte de verdade: uma indisponibilidade de cache degrada performance, nunca
     * disponibilidade. Se estes testes começarem a falhar, alguém transformou o cache num
     * ponto único de falha.
     */
    @Test
    @DisplayName("Redis fora do ar vira miss, não exception")
    void readFailsOpen() {
        when(valueOps.get(any())).thenThrow(new QueryTimeoutException("redis fora"));

        assertThat(cache.get("k", Dto.class)).isEmpty();
    }

    @Test
    @DisplayName("payload ilegível também vira miss")
    void unreadablePayloadIsMiss() {
        when(valueOps.get("crm:k")).thenReturn("isto não é json");

        assertThat(cache.get("k", Dto.class)).isEmpty();
    }

    @Test
    @DisplayName("falha ao gravar é engolida")
    void writeFailsOpen() {
        doThrow(new QueryTimeoutException("redis fora"))
                .when(valueOps).set(any(), any(), any(Duration.class));

        assertThatCode(() -> cache.put("k", new Dto("x", 1), Duration.ofMinutes(1)))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("falha ao invalidar é engolida")
    void evictFailsOpen() {
        when(redisTemplate.delete(any(String.class))).thenThrow(new QueryTimeoutException("redis fora"));

        assertThatCode(() -> cache.evict("k")).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("getOrLoad devolve o valor cacheado sem chamar o loader")
    void getOrLoadUsesCache() {
        when(valueOps.get("crm:k")).thenReturn("{\"name\":\"lote 1\",\"quantity\":3}");

        var calls = new AtomicInteger();

        var result = cache.getOrLoad("k", Dto.class, Duration.ofMinutes(1), () -> {
            calls.incrementAndGet();
            return new Dto("do banco", 9);
        });

        assertThat(result).isEqualTo(new Dto("lote 1", 3));
        assertThat(calls.get()).isZero();
    }

    @Test
    @DisplayName("getOrLoad cai no loader quando o cache está fora, e ainda assim responde")
    void getOrLoadFallsBackWhenCacheIsDown() {
        when(valueOps.get(any())).thenThrow(new QueryTimeoutException("redis fora"));
        doThrow(new QueryTimeoutException("redis fora"))
                .when(valueOps).set(any(), any(), any(Duration.class));

        var result = cache.getOrLoad("k", Dto.class, Duration.ofMinutes(1), () -> new Dto("do banco", 9));

        assertThat(result).isEqualTo(new Dto("do banco", 9));
    }

    @Test
    @DisplayName("getOrLoad não cacheia null")
    void getOrLoadDoesNotCacheNull() {
        when(valueOps.get(any())).thenReturn(null);

        var result = cache.getOrLoad("k", Dto.class, Duration.ofMinutes(1), () -> null);

        assertThat(result).isNull();
        verify(valueOps, never()).set(any(), any(), any(Duration.class));
    }

    @Test
    @DisplayName("TTL ausente ou não positivo cai no default — nunca grava entrada eterna")
    void fallsBackToDefaultTtl() {
        cache.put("a", new Dto("x", 1), null);
        cache.put("b", new Dto("x", 1), Duration.ZERO);
        cache.put("c", new Dto("x", 1), Duration.ofSeconds(-5));

        verify(valueOps).set(eq("crm:a"), any(), eq(PROPERTIES.defaultTtl()));
        verify(valueOps).set(eq("crm:b"), any(), eq(PROPERTIES.defaultTtl()));
        verify(valueOps).set(eq("crm:c"), any(), eq(PROPERTIES.defaultTtl()));
    }
}
