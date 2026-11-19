package com.crm.common.infrastructure.events;

import com.crm.common.application.events.EventPublisher;
import com.crm.common.application.outbox.OutboxWriter;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Liga o {@link EventPublisher}: evento local pelo Spring e integration event pelo outbox.
 */
@Configuration
public class EventsConfig {

    @Bean
    public EventPublisher eventPublisher(
            ApplicationEventPublisher localPublisher,
            OutboxWriter outboxWriter
    ) {
        return new SpringEventPublisherImpl(localPublisher, outboxWriter);
    }
}
