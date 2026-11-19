package com.crm.common.infrastructure.events;

import com.crm.common.application.events.EventPublisher;
import com.crm.common.application.outbox.OutboxWriter;
import com.crm.common.domain.events.DomainEvent;
import com.crm.common.domain.events.IntegrationEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Ponto único de saída de evento da application. <b>Roteia por tipo:</b>
 *
 * <ul>
 *   <li>{@link IntegrationEvent} — cruza bounded context, então vai para o <b>outbox</b>, na
 *       mesma transação do aggregate. Não é publicado localmente.
 *   <li>qualquer outro {@link DomainEvent} — fica dentro do BC, publicado in-process pelo
 *       Spring, <b>só depois do commit</b>.
 * </ul>
 *
 * <p>Entregar o domain event depois do commit é o que impede um listener de reagir a um estado
 * que ainda pode ser desfeito (ex. mandar o e-mail de um token cuja gravação foi revertida).
 * Consistência imediata entre aggregates não passa por evento: é escrita explicitamente no use
 * case (docs/architecture.md §6.1). Sem transação ativa, a entrega é imediata.
 *
 * <p>O chamador não decide o caminho, e é isso que se quer: o use case só chama
 * {@code publishAll(aggregate.pullEvents())} e o roteamento segue do tipo do evento.
 */
@RequiredArgsConstructor
class SpringEventPublisherImpl implements EventPublisher {

    private final ApplicationEventPublisher localPublisher;
    private final OutboxWriter outboxWriter;

    @Override
    public void publish(DomainEvent event) {
        if (event instanceof IntegrationEvent integrationEvent) {
            outboxWriter.save(integrationEvent);
            return;
        }

        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            localPublisher.publishEvent(event);
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                localPublisher.publishEvent(event);
            }
        });
    }
}
