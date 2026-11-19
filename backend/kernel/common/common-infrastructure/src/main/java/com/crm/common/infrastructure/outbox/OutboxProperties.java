package com.crm.common.infrastructure.outbox;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

/**
 * Ajustes do outbox, todos com default utilizável — subir o app não exige configurar nada.
 *
 * <pre>
 * crm.outbox.batch-size=100
 * crm.outbox.max-attempts=5
 * crm.outbox.retry-backoff=10s
 * crm.outbox.poll-delay=5s
 * </pre>
 *
 * <p>Este record é a lista completa dos botões do outbox, e isso é intencional: {@code pollDelay}
 * já morou inline numa string de {@code @Scheduled}, o que fazia quem lesse esta classe achar
 * que tinha visto tudo. O agendamento agora é programático ({@code OutboxSchedulingConfig})
 * justamente para que exista um único lugar declarando esses valores.
 *
 * @param batchSize    quantos eventos um ciclo de despacho pega por vez
 * @param maxAttempts  tentativas antes de o evento virar DEAD e sair da fila
 * @param retryBackoff base do backoff exponencial: base * 2^(tentativas - 1)
 * @param pollDelay    intervalo entre o fim de um ciclo de despacho e o início do próximo
 */
@ConfigurationProperties(prefix = "crm.outbox")
public record OutboxProperties(
        @DefaultValue("100") int batchSize,
        @DefaultValue("5") int maxAttempts,
        @DefaultValue("10s") Duration retryBackoff,
        @DefaultValue("5s") Duration pollDelay
) {
}
