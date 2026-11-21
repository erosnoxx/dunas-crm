package com.crm.iam.application.usecases.commands;

import com.crm.common.application.annotations.UseCase;
import com.crm.common.application.events.EventPublisher;
import com.crm.common.application.exceptions.NotFoundException;
import com.crm.iam.application.Actor;
import com.crm.iam.application.contracts.repositories.SessionRepository;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.Instant;

/**
 * Encerra a sessão do próprio {@link Actor}. Idempotente: a segunda chamada não publica de novo.
 */
@UseCase
@RequiredArgsConstructor
public class Logout {

    private final SessionRepository sessionRepository;
    private final EventPublisher publisher;
    private final Clock clock;

    public void execute(Actor actor) {
        var session = sessionRepository.findById(actor.sessionId())
                .orElseThrow(() -> new NotFoundException("session_not_found", "sessão não encontrada"));

        session.revoke(actor.userId(), Instant.now(clock));

        sessionRepository.save(session);

        publisher.publishAll(session.pullEvents());
    }
}
