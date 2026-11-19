package com.crm.common.infrastructure.persistence;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Linha da tabela de deduplicação. Não estende {@code JpaEntity}: a chave de negócio (a própria
 * chave de idempotência) já é a PK, e não existe trilha de auditoria além de
 * {@code createdAt}/{@code completedAt}, que já contam a história inteira.
 *
 * <p>A transição {@code FAILED → IN_PROGRESS} (permitir nova tentativa) não é um método desta
 * classe — precisa ser um {@code UPDATE ... WHERE status = 'FAILED'} atômico no banco
 * ({@link IdempotentExecutionJpaRepository#beginRetry}), porque carregar a entidade, mutar em
 * memória e salvar teria uma janela de corrida entre duas tentativas concorrentes lendo o mesmo
 * estado {@code FAILED}.
 */
@Entity
@Table(name = "idempotent_execution")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class IdempotentExecutionEntity {

    @Id
    private String id;

    @Column(nullable = false, updatable = false)
    private String requestFingerprint;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IdempotentExecutionStatus status;

    private String resultPayload;

    private String failureReason;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    private Instant completedAt;

    IdempotentExecutionEntity(
            String id,
            String requestFingerprint,
            Instant now
    ) {
        this.id = id;
        this.requestFingerprint = requestFingerprint;
        this.status = IdempotentExecutionStatus.IN_PROGRESS;
        this.createdAt = now;
    }

    void markCompleted(String resultPayload, Instant now) {
        this.status = IdempotentExecutionStatus.COMPLETED;
        this.resultPayload = resultPayload;
        this.failureReason = null;
        this.completedAt = now;
    }

    void markFailed(String reason, Instant now) {
        this.status = IdempotentExecutionStatus.FAILED;
        this.failureReason = reason;
        this.completedAt = now;
    }

    boolean isCompleted() {
        return status == IdempotentExecutionStatus.COMPLETED;
    }

    boolean isInProgress() {
        return status == IdempotentExecutionStatus.IN_PROGRESS;
    }

    boolean matchesFingerprint(String otherFingerprint) {
        return requestFingerprint.equals(otherFingerprint);
    }
}
