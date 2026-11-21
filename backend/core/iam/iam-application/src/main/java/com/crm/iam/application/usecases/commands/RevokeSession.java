package com.crm.iam.application.usecases.commands;

import com.crm.common.application.annotations.UseCase;
import com.crm.common.application.events.EventPublisher;
import com.crm.common.application.exceptions.NotFoundException;
import com.crm.iam.application.Actor;
import com.crm.iam.application.contracts.repositories.SessionRepository;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

/**
 * Revoga uma sessão: a do próprio {@link Actor} ou, se ele é Administrador, a de qualquer um.
 * Idempotente: a segunda chamada não publica de novo.
 */
@UseCase
@RequiredArgsConstructor
public class RevokeSession {

    private final SessionRepository sessionRepository;
    private final EventPublisher publisher;
    private final Clock clock;

    /**
     * @param actor     quem revoga
     * @param sessionId sessão revogada
     */
    public record Params(
            Actor actor,
            UUID sessionId
    ) {
    }

    public void execute(Params params) {
        var actor = params.actor();

        var session = sessionRepository.findById(params.sessionId())
                .orElseThrow(() -> new NotFoundException("session_not_found", "sessão não encontrada"));

        if (!session.getUserId().equals(actor.userId()))
            actor.requireAdministrator();

        session.revoke(actor.userId(), Instant.now(clock));

        sessionRepository.save(session);

        publisher.publishAll(session.pullEvents());
    }
}
