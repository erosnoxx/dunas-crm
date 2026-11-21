package com.crm.iam.application.usecases.commands;

import com.crm.common.application.annotations.UseCase;
import com.crm.common.application.events.EventPublisher;
import com.crm.common.application.exceptions.BadRequestException;
import com.crm.iam.application.Actor;
import com.crm.iam.application.contracts.repositories.SessionRepository;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

/**
 * Revoga as sessões ativas de um usuário: as do próprio {@link Actor} ou, se ele é Administrador, as
 * de qualquer um. {@code keepCurrent} mantém a sessão do {@link Actor} e só vale para o próprio
 * usuário.
 */
@UseCase
@RequiredArgsConstructor
public class RevokeAllSessions {

    private final SessionRepository sessionRepository;
    private final EventPublisher publisher;
    private final Clock clock;

    /**
     * @param actor       quem revoga
     * @param userId      dono das sessões
     * @param keepCurrent {@code true} mantém a sessão do {@code actor}
     */
    public record Params(
            Actor actor,
            UUID userId,
            boolean keepCurrent
    ) {
    }

    public void execute(Params params) {
        var actor = params.actor();
        var own = actor.userId().equals(params.userId());

        if (!own)
            actor.requireAdministrator();

        if (params.keepCurrent() && !own)
            throw new BadRequestException("manter a sessão atual só vale para o próprio usuário");

        var now = Instant.now(clock);

        for (var session : sessionRepository.findActiveByUserId(params.userId(), now)) {
            if (params.keepCurrent() && session.getId().equals(actor.sessionId()))
                continue;

            session.revoke(actor.userId(), now);

            sessionRepository.save(session);

            publisher.publishAll(session.pullEvents());
        }
    }
}
