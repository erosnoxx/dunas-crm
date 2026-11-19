package com.crm.common.infrastructure.outbox.persistence;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Linha da tabela de outbox.
 *
 * <p>Não estende {@code JpaEntity}: não tem trilha de auditoria (createdAt/updatedAt) porque
 * {@code occurredOn} e {@code processedAt} já contam a história inteira, e a linha é
 * descartável depois de processada.
 *
 * <p>Sem setter público: as transições de estado são métodos nomeados, para que a regra de
 * retry/desistência viva num lugar só.
 */
@Entity
@Table(name = "outbox_event")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class OutboxEventEntity {

    @Id
    private UUID id;

    @Column(nullable = false, updatable = false)
    private UUID aggregateId;

    @Column(nullable = false, updatable = false)
    private String eventType;

    // sem @Lob de propósito: no Postgres o Hibernate mapearia @Lob para large object (oid),
    // não para text, e a coluna da migration é text.
    @Column(nullable = false, updatable = false)
    private String payload;

    @Column(nullable = false, updatable = false)
    private Instant occurredOn;

    // nulo só nas linhas anteriores à migration V3; todo evento novo grava o id da correlação.
    @Column(updatable = false)
    private String correlationId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OutboxEventStatus status;

    @Column(nullable = false)
    private int attemptCount;

    private Instant nextRetryAt;

    private Instant processedAt;

    private String failureReason;

    OutboxEventEntity(
            UUID id,
            UUID aggregateId,
            String eventType,
            String payload,
            Instant occurredOn,
            String correlationId
    ) {
        this.id = id;
        this.aggregateId = aggregateId;
        this.eventType = eventType;
        this.payload = payload;
        this.occurredOn = occurredOn;
        this.correlationId = correlationId;
        this.status = OutboxEventStatus.PENDING;
        this.attemptCount = 0;
    }

    void markProcessed(Instant now) {
        this.status = OutboxEventStatus.PROCESSED;
        this.processedAt = now;
        this.nextRetryAt = null;
        this.failureReason = null;
    }

    /**
     * Registra a falha e decide entre reagendar ou desistir.
     *
     * @return true quando o evento passou a DEAD nesta chamada — o chamador usa isso para
     *         logar em ERROR, já que desistir de um evento é um fato operacional, não rotina.
     */
    boolean markFailed(
            String reason,
            int maxAttempts,
            Duration retryBackoff,
            Instant now
    ) {
        // já desistiu: não conta tentativa nem sinaliza de novo. Na prática o despachante nunca
        // seleciona uma linha DEAD, mas isso mantém o retorno honesto — ele significa "passou a
        // DEAD agora", e é o que faz o chamador logar em ERROR uma vez, não a cada ciclo.
        if (status == OutboxEventStatus.DEAD)
            return false;

        this.attemptCount = attemptCount + 1;
        this.failureReason = reason;

        if (attemptCount >= maxAttempts) {
            this.status = OutboxEventStatus.DEAD;
            this.nextRetryAt = null;
            return true;
        }

        this.nextRetryAt = now.plusSeconds(backoffSeconds(retryBackoff));
        return false;
    }

    /** Backoff exponencial sobre a base configurada: base * 2^(tentativas - 1). */
    private long backoffSeconds(Duration retryBackoff) {
        var multiplier = 1L << (attemptCount - 1);

        return retryBackoff.toSeconds() * multiplier;
    }
}
