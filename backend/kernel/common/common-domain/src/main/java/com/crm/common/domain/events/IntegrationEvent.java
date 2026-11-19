package com.crm.common.domain.events;

/**
 * {@link DomainEvent} que atravessa a fronteira do bounded context.
 *
 * <p>É gravado no outbox e entregue ao broker, para os outros BCs reagirem sem import direto.
 */
public interface IntegrationEvent extends DomainEvent {
    String eventType();
}
