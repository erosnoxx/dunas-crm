package com.crm.common.infrastructure.outbox.persistence;

/**
 * Ciclo de vida de uma linha do outbox.
 *
 * <p>Só {@link #PENDING} é elegível para despacho. {@link #DEAD} existe para que um evento
 * permanentemente quebrado saia da fila em vez de reaparecer a cada backoff para sempre —
 * sem ele, ninguém descobre que um evento nunca foi entregue.
 */
enum OutboxEventStatus {
    PENDING,
    PROCESSED,
    DEAD
}
