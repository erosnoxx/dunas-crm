package com.crm.iam.domain.events;

import com.crm.common.domain.events.DomainEvent;

import java.time.Instant;
import java.util.UUID;

/**
 * O papel de uma conta mudou. O listener invalida o cache de sessão do usuário, porque o papel
 * vai junto da sessão em cache.
 */
public record UserRoleChanged(
        UUID aggregateId,
        Instant occurredOn
) implements DomainEvent {
}
