package com.crm.common.infrastructure.outbox.persistence;

import com.crm.common.application.outbox.OutboxEvent;
import com.crm.common.application.outbox.OutboxReader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Lado de leitura e de resultado do outbox, usado só pelo worker. Nenhum use case de bounded
 * context enxerga isto — a application do produtor só conhece o {@code OutboxWriter}.
 */
@Slf4j
@RequiredArgsConstructor
class JpaOutboxReaderImpl implements OutboxReader {

    private final OutboxEventJpaRepository repository;
    private final int maxAttempts;
    private final Duration retryBackoff;
    private final Clock clock;

    @Override
    public List<OutboxEvent> findPending(int limit) {
        return repository.findDispatchable(Instant.now(clock), PageRequest.of(0, limit))
                .stream()
                .map(this::toRecord)
                .toList();
    }

    @Override
    public void markAsProcessed(UUID eventId) {
        var entity = findOrThrow(eventId);

        entity.markProcessed(Instant.now(clock));

        repository.save(entity);
    }

    @Override
    public void markAsFailed(UUID eventId, String reason) {
        var entity = findOrThrow(eventId);

        var gaveUp = entity.markFailed(
                reason,
                maxAttempts,
                retryBackoff,
                Instant.now(clock)
        );

        repository.save(entity);

        if (gaveUp)
            log.error(
                    "evento de outbox {} ({}) descartado após {} tentativas — motivo: {}",
                    eventId, entity.getEventType(), entity.getAttemptCount(), reason
            );
    }

    @Override
    public void deleteProcessedOlderThan(Instant cutoff) {
        repository.deleteProcessedBefore(cutoff);
    }

    private OutboxEventEntity findOrThrow(UUID eventId) {
        return repository.findById(eventId)
                .orElseThrow(() -> new OutboxEventNotFoundException(
                        "evento de outbox não encontrado: " + eventId
                ));
    }

    private OutboxEvent toRecord(OutboxEventEntity entity) {
        return new OutboxEvent(
                entity.getId(),
                entity.getAggregateId(),
                entity.getEventType(),
                entity.getPayload(),
                entity.getOccurredOn(),
                entity.getAttemptCount(),
                entity.getCorrelationId()
        );
    }
}
