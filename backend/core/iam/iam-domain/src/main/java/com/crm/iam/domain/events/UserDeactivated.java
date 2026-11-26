package com.crm.iam.domain.events;

import com.crm.common.domain.events.DomainEvent;

import java.time.Instant;
import java.util.UUID;

/**
 * Uma conta foi desativada. O listener revoga as sessões do usuário.
 */
public record UserDeactivated(
        UUID aggregateId,
        Instant occurredOn
) implements DomainEvent {
}
