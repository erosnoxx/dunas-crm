package com.crm.common.infrastructure.persistence;

/**
 * Ciclo de vida de uma linha de {@code idempotent_execution}.
 *
 * <p>{@code FAILED} não é terminal, diferente do {@code DEAD} do outbox: nada foi de fato
 * completado, então a próxima chamada com a mesma chave tem permissão de tentar de novo — ver
 * {@link IdempotentExecutionEntity#beginRetry}.
 */
enum IdempotentExecutionStatus {
    IN_PROGRESS,
    COMPLETED,
    FAILED
}
