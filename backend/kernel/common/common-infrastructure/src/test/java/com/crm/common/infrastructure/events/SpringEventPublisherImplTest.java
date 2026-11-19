package com.crm.common.infrastructure.events;

import com.crm.common.application.outbox.OutboxWriter;
import com.crm.common.domain.events.DomainEvent;
import com.crm.common.domain.events.IntegrationEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("SpringEventPublisherImpl")
class SpringEventPublisherImplTest {

    private static final Instant NOW = Instant.parse("2026-10-01T15:00:00Z");

    private record LocalEvent(UUID aggregateId, Instant occurredOn) implements DomainEvent {
    }

    private record CrossContextEvent(
            UUID aggregateId,
            Instant occurredOn,
            String eventType
    ) implements IntegrationEvent {
    }

    private ApplicationEventPublisher localPublisher;
    private OutboxWriter outboxWriter;
    private SpringEventPublisherImpl publisher;

    @BeforeEach
    void setUp() {
        localPublisher = mock(ApplicationEventPublisher.class);
        outboxWriter = mock(OutboxWriter.class);

        publisher = new SpringEventPublisherImpl(localPublisher, outboxWriter);
    }

    @AfterEach
    void clearTransaction() {
        if (TransactionSynchronizationManager.isSynchronizationActive())
            TransactionSynchronizationManager.clearSynchronization();
    }

    @Test
    @DisplayName("sem transação ativa, domain event é publicado in-process na hora")
    void domainEventGoesLocalWithoutTransaction() {
        var event = new LocalEvent(UUID.randomUUID(), NOW);

        publisher.publish(event);

        verify(localPublisher).publishEvent(event);
        verifyNoInteractions(outboxWriter);
    }

    @Test
    @DisplayName("com transação ativa, domain event só é publicado depois do commit")
    void domainEventWaitsForCommit() {
        TransactionSynchronizationManager.initSynchronization();
        var event = new LocalEvent(UUID.randomUUID(), NOW);

        publisher.publish(event);

        verify(localPublisher, never()).publishEvent(any(Object.class));

        TransactionSynchronizationManager.getSynchronizations()
                .forEach(TransactionSynchronization::afterCommit);

        verify(localPublisher).publishEvent(event);
    }

    @Test
    @DisplayName("com transação revertida, domain event nunca é publicado")
    void domainEventDroppedOnRollback() {
        TransactionSynchronizationManager.initSynchronization();
        var event = new LocalEvent(UUID.randomUUID(), NOW);

        publisher.publish(event);

        TransactionSynchronizationManager.getSynchronizations()
                .forEach(sync -> sync.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));

        verify(localPublisher, never()).publishEvent(any(Object.class));
    }

    /**
     * O caminho de integração é <b>só</b> o outbox: publicar localmente também faria o evento
     * ser entregue duas vezes por rotas diferentes, uma delas sem a garantia transacional.
     */
    @Test
    @DisplayName("integration event vai só para o outbox, nunca local")
    void integrationEventGoesOnlyToOutbox() {
        var event = new CrossContextEvent(UUID.randomUUID(), NOW, "ordering.order.paid");

        publisher.publish(event);

        verify(outboxWriter).save(event);
        verify(localPublisher, never()).publishEvent(any(Object.class));
    }

    /**
     * O outbox precisa gravar dentro da transação do aggregate — é isso que elimina o
     * dual-write. Adiar para depois do commit quebraria a garantia.
     */
    @Test
    @DisplayName("com transação ativa, integration event vai para o outbox na hora, dentro da transação")
    void integrationEventStaysInsideTransaction() {
        TransactionSynchronizationManager.initSynchronization();
        var event = new CrossContextEvent(UUID.randomUUID(), NOW, "ordering.order.paid");

        publisher.publish(event);

        verify(outboxWriter).save(event);
    }

    @Test
    @DisplayName("publishAll roteia cada evento pelo seu próprio tipo")
    void publishAllRoutesEachEvent() {
        var local = new LocalEvent(UUID.randomUUID(), NOW);
        var integration = new CrossContextEvent(UUID.randomUUID(), NOW, "iam.user.created");

        publisher.publishAll(List.of(local, integration));

        verify(localPublisher).publishEvent(local);
        verify(outboxWriter).save(integration);
    }
}
