package com.crm.common.infrastructure.outbox.persistence;

import com.crm.common.application.outbox.OutboxWriter;
import com.crm.common.domain.events.IntegrationEvent;
import com.crm.common.infrastructure.observability.CorrelationContext;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

/**
 * Grava o evento de integração na mesma transação do aggregate — é o que elimina o dual-write:
 * ou o estado e o evento persistem juntos, ou nenhum dos dois.
 *
 * <p>Não abre transação própria de propósito. Ela vem de quem chamou (o use case, via o aspect
 * de {@code @UseCase}). Abrir uma aqui quebraria exatamente a garantia que este writer existe
 * para dar.
 */
@RequiredArgsConstructor
class JpaOutboxWriterImpl implements OutboxWriter {

    private final OutboxEventJpaRepository repository;
    private final ObjectMapper objectMapper;

    @Override
    public void save(IntegrationEvent event) {
        var entity = new OutboxEventEntity(
                UUID.randomUUID(),
                event.aggregateId(),
                event.eventType(),
                serialize(event),
                event.occurredOn(),
                correlationIdOfThisWork()
        );

        repository.save(entity);
    }

    /**
     * O id é lido do MDC aqui, na thread que grava, e não na do despachante: é nesta que a
     * requisição (ou o job) está. Sem contexto (um teste, um seed), o evento vira a raiz de uma
     * correlação nova em vez de ficar sem id.
     */
    private String correlationIdOfThisWork() {
        return CorrelationContext.correlationId().orElseGet(CorrelationContext::newId);
    }

    private String serialize(IntegrationEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        }

        catch (JsonProcessingException e) {
            throw new EventSerializationException("falha ao serializar evento de integração", e);
        }
    }
}
