package com.crm.iam.domain.events;

import com.crm.common.domain.events.DomainEvent;
import com.crm.iam.domain.enums.TokenPurpose;
import com.crm.iam.domain.vos.TokenSecret;

import java.time.Instant;
import java.util.UUID;

/**
 * Um link de uso único foi emitido. O listener de {@code iam-infrastructure} monta o e-mail com o
 * link e o envia depois do commit (ADR-7).
 *
 * <p>É o único evento que carrega um segredo, então é só domain event: nunca vai ao outbox nem ao
 * broker, e nunca é logado nem serializado. O {@code toString} do record mostra o segredo
 * mascarado, porque {@link TokenSecret#toString()} nunca devolve o valor.
 */
public record OneTimeTokenIssued(
        UUID aggregateId,
        Instant occurredOn,
        UUID userId,
        TokenPurpose purpose,
        TokenSecret secret
) implements DomainEvent {
}
