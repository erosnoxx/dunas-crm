package com.crm.common.infrastructure.messaging;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

/**
 * Ajustes do transporte de integration events.
 *
 * <pre>
 * crm.messaging.exchange=crm.events
 * crm.messaging.confirm-timeout=5s
 * </pre>
 *
 * @param exchange       topic exchange onde todo integration event é publicado. A routing key
 *                       é o eventType do evento, então cada BC consumidor declara a própria
 *                       queue e o próprio binding, na infraestrutura dele.
 * @param confirmTimeout quanto esperar pelo publisher confirm do broker antes de considerar a
 *                       publicação não confirmada.
 */
@ConfigurationProperties(prefix = "crm.messaging")
public record MessagingProperties(
        @DefaultValue("crm.events") String exchange,
        @DefaultValue("5s") Duration confirmTimeout
) {
}
