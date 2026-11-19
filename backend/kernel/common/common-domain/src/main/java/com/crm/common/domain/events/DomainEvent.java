package com.crm.common.domain.events;

import java.time.Instant;
import java.util.UUID;

/**
 * Algo que aconteceu no domínio, no passado, e que outras partes do mesmo bounded context podem
 * querer ouvir.
 */
public interface DomainEvent {
    UUID aggregateId();
    Instant occurredOn();
}
