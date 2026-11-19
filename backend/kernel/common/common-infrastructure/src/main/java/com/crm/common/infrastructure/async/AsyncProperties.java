package com.crm.common.infrastructure.async;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

/**
 * Ajustes do executor de efeitos colaterais.
 *
 * <pre>
 * crm.async.core-pool-size=2
 * crm.async.max-pool-size=8
 * crm.async.queue-capacity=500
 * crm.async.shutdown-timeout=30s
 * </pre>
 */
@ConfigurationProperties(prefix = "crm.async")
public record AsyncProperties(
        @DefaultValue("2") int corePoolSize,
        @DefaultValue("8") int maxPoolSize,
        @DefaultValue("500") int queueCapacity,
        @DefaultValue("30s") Duration shutdownTimeout
) {
}
