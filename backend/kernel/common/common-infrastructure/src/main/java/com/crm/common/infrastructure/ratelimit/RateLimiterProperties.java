package com.crm.common.infrastructure.ratelimit;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Ajustes do limite de tentativas.
 *
 * <pre>
 * crm.rate-limit.key-prefix=crm:ratelimit:
 * </pre>
 *
 * @param keyPrefix prefixo de toda chave de contador, para isolar o namespace dentro de um Redis
 *                  compartilhado
 */
@ConfigurationProperties(prefix = "crm.rate-limit")
public record RateLimiterProperties(
        @DefaultValue("crm:ratelimit:") String keyPrefix
) {
}
