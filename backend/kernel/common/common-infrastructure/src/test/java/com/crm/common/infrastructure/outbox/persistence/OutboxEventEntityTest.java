package com.crm.common.infrastructure.outbox.persistence;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("OutboxEventEntity")
class OutboxEventEntityTest {

    private static final Duration BACKOFF = Duration.ofSeconds(10);
    private static final int MAX_ATTEMPTS = 3;
    private static final Instant NOW = Instant.parse("2026-10-01T15:00:00Z");

    private OutboxEventEntity newEvent() {
        return new OutboxEventEntity(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "ordering.order.paid",
                "{}",
                NOW,
                "corr-1"
        );
    }

    @Test
    @DisplayName("guarda o correlationId com que foi criado")
    void keepsTheCorrelationId() {
        assertThat(newEvent().getCorrelationId()).isEqualTo("corr-1");
    }

    @Test
    @DisplayName("nasce PENDING, sem tentativas e elegível imediatamente")
    void startsPending() {
        var event = newEvent();

        assertThat(event.getStatus()).isEqualTo(OutboxEventStatus.PENDING);
        assertThat(event.getAttemptCount()).isZero();
        assertThat(event.getNextRetryAt()).isNull();
        assertThat(event.getProcessedAt()).isNull();
    }

    @Test
    @DisplayName("markProcessed registra a hora recebida e limpa a janela de retry e o motivo da falha anterior")
    void markProcessedClearsRetryState() {
        var event = newEvent();

        event.markFailed("timeout", MAX_ATTEMPTS, BACKOFF, NOW);
        event.markProcessed(NOW.plusSeconds(30));

        assertThat(event.getStatus()).isEqualTo(OutboxEventStatus.PROCESSED);
        assertThat(event.getProcessedAt()).isEqualTo(NOW.plusSeconds(30));
        assertThat(event.getNextRetryAt()).isNull();
        assertThat(event.getFailureReason()).isNull();
    }

    @Test
    @DisplayName("markFailed conta a tentativa, guarda o motivo e reagenda a partir da hora recebida")
    void markFailedReschedules() {
        var event = newEvent();

        var gaveUp = event.markFailed("broker recusou", MAX_ATTEMPTS, BACKOFF, NOW);

        assertThat(gaveUp).isFalse();
        assertThat(event.getStatus()).isEqualTo(OutboxEventStatus.PENDING);
        assertThat(event.getAttemptCount()).isEqualTo(1);
        assertThat(event.getFailureReason()).isEqualTo("broker recusou");
        assertThat(event.getNextRetryAt()).isEqualTo(NOW.plus(BACKOFF));
    }

    @Test
    @DisplayName("o backoff dobra a cada tentativa")
    void backoffGrowsExponentially() {
        var event = newEvent();

        // base * 2^(tentativas-1) = 10s, 20s, 40s
        event.markFailed("falha", 10, BACKOFF, NOW);
        assertThat(event.getNextRetryAt()).isEqualTo(NOW.plusSeconds(10));

        event.markFailed("falha", 10, BACKOFF, NOW);
        assertThat(event.getNextRetryAt()).isEqualTo(NOW.plusSeconds(20));

        event.markFailed("falha", 10, BACKOFF, NOW);
        assertThat(event.getNextRetryAt()).isEqualTo(NOW.plusSeconds(40));
    }

    @Test
    @DisplayName("vira DEAD ao atingir maxAttempts e sai da fila")
    void becomesDeadAtMaxAttempts() {
        var event = newEvent();

        assertThat(event.markFailed("falha", MAX_ATTEMPTS, BACKOFF, NOW)).isFalse();
        assertThat(event.markFailed("falha", MAX_ATTEMPTS, BACKOFF, NOW)).isFalse();

        var gaveUp = event.markFailed("falha definitiva", MAX_ATTEMPTS, BACKOFF, NOW);

        assertThat(gaveUp).isTrue();
        assertThat(event.getStatus()).isEqualTo(OutboxEventStatus.DEAD);
        assertThat(event.getAttemptCount()).isEqualTo(MAX_ATTEMPTS);
        assertThat(event.getFailureReason()).isEqualTo("falha definitiva");

        // nextRetryAt nulo é o que tira a linha do caminho do despachante
        assertThat(event.getNextRetryAt()).isNull();
    }

    /**
     * O retorno existe para o chamador logar em ERROR só na transição. Se sinalizasse em toda
     * falha depois de morto, o log viraria ruído.
     */
    @Test
    @DisplayName("sinaliza a desistência só na transição para DEAD")
    void signalsOnlyOnTransition() {
        var event = newEvent();

        assertThat(event.markFailed("falha", 1, BACKOFF, NOW)).isTrue();
        assertThat(event.getStatus()).isEqualTo(OutboxEventStatus.DEAD);

        // segunda falha sobre um evento já morto não sinaliza de novo nem conta tentativa
        assertThat(event.markFailed("de novo", 1, BACKOFF, NOW)).isFalse();
        assertThat(event.getAttemptCount()).isEqualTo(1);
        assertThat(event.getFailureReason()).isEqualTo("falha");
    }
}
