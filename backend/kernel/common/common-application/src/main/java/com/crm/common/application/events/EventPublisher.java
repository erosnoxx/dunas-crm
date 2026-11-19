package com.crm.common.application.events;


import com.crm.common.domain.events.DomainEvent;

import java.util.Collection;

/**
 * Publica os eventos que o aggregate registrou.
 *
 * <p>Quem chama é a camada de application, <b>depois</b> que o save foi confirmado — o
 * aggregate só registra, e o repository não publica nada (ver docs/architecture.md §5.1).
 */
public interface EventPublisher {
    void publish(DomainEvent event);

    /**
     * Uso típico no use case: {@code publisher.publishAll(aggregate.pullEvents())}.
     *
     * <p>{@code pullEvents()} já limpa a lista do aggregate, então um segundo save da mesma
     * instância não republica nada.
     */
    default void publishAll(Collection<? extends DomainEvent> events) {
        events.forEach(this::publish);
    }
}
