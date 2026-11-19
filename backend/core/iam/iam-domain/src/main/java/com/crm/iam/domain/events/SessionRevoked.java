package com.crm.iam.domain.events;

import com.crm.common.domain.events.DomainEvent;
import com.crm.iam.domain.vos.TokenHash;

import java.time.Instant;
import java.util.UUID;

/**
 * Uma sessão foi encerrada. O listener tira a sessão do cache pelo hash na hora, o que torna o
 * encerramento remoto imediato (ADR-1). Carrega só o {@link TokenHash}, nunca o segredo.
 */
public record SessionRevoked(UUID aggregateId, Instant occurredOn, TokenHash tokenHash) implements DomainEvent {
}
