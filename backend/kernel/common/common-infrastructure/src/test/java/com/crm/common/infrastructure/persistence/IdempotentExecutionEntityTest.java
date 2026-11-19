package com.crm.common.infrastructure.persistence;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("IdempotentExecutionEntity")
class IdempotentExecutionEntityTest {

    private static final Instant NOW = Instant.parse("2026-10-01T15:00:00Z");

    @Test
    @DisplayName("nasce IN_PROGRESS, com a hora recebida, sem resultado nem motivo de falha")
    void startsInProgress() {
        var entity = new IdempotentExecutionEntity("key-1", "fingerprint-hash", NOW);

        assertThat(entity.getId()).isEqualTo("key-1");
        assertThat(entity.getRequestFingerprint()).isEqualTo("fingerprint-hash");
        assertThat(entity.getCreatedAt()).isEqualTo(NOW);
        assertThat(entity.isInProgress()).isTrue();
        assertThat(entity.isCompleted()).isFalse();
        assertThat(entity.getResultPayload()).isNull();
        assertThat(entity.getCompletedAt()).isNull();
    }

    @Test
    @DisplayName("markCompleted grava o payload, a hora recebida e limpa motivo de falha anterior")
    void markCompletedStoresPayload() {
        var entity = new IdempotentExecutionEntity("key-1", "fingerprint-hash", NOW);

        entity.markFailed("timeout", NOW.plusSeconds(1));
        entity.markCompleted("{\"orderId\":\"abc\"}", NOW.plusSeconds(2));

        assertThat(entity.isCompleted()).isTrue();
        assertThat(entity.getResultPayload()).isEqualTo("{\"orderId\":\"abc\"}");
        assertThat(entity.getFailureReason()).isNull();
        assertThat(entity.getCompletedAt()).isEqualTo(NOW.plusSeconds(2));
    }

    @Test
    @DisplayName("markFailed grava o motivo e a hora recebida, sem payload")
    void markFailedStoresReason() {
        var entity = new IdempotentExecutionEntity("key-1", "fingerprint-hash", NOW);

        entity.markFailed("gateway indisponível", NOW.plusSeconds(1));

        assertThat(entity.isInProgress()).isFalse();
        assertThat(entity.isCompleted()).isFalse();
        assertThat(entity.getFailureReason()).isEqualTo("gateway indisponível");
        assertThat(entity.getCompletedAt()).isEqualTo(NOW.plusSeconds(1));
    }

    @Test
    @DisplayName("matchesFingerprint compara por igualdade exata")
    void matchesFingerprintComparesExactly() {
        var entity = new IdempotentExecutionEntity("key-1", "abc123", NOW);

        assertThat(entity.matchesFingerprint("abc123")).isTrue();
        assertThat(entity.matchesFingerprint("outro-hash")).isFalse();
    }
}
