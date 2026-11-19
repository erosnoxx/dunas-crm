package com.crm.common.application.outbox;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Lado de leitura do outbox, usado pelo job que despacha os eventos pendentes.
 */
public interface OutboxReader {
    List<OutboxEvent> findPending(int limit);
    void markAsProcessed(UUID eventId);
    void markAsFailed(UUID eventId, String reason);
    void deleteProcessedOlderThan(Instant cutOff);
}
