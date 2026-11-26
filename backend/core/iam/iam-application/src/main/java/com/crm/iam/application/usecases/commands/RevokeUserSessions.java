package com.crm.iam.application.usecases.commands;

import com.crm.common.application.annotations.UseCase;
import com.crm.common.application.events.EventPublisher;
import com.crm.iam.application.contracts.repositories.SessionRepository;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

/**
 * Revoga todas as sessões ativas de um usuário. É interno: o listener de {@code UserDeactivated}
 * o chama, sem {@code Actor} e sem checagem de papel, porque a permissão já foi checada em
 * {@link DeactivateUser}.
 */
@UseCase
@RequiredArgsConstructor
public class RevokeUserSessions {

    private final SessionRepository sessionRepository;
    private final EventPublisher publisher;
    private final Clock clock;

    /**
     * @param userId    dono das sessões
     * @param revokedBy autor da desativação
     */
    public record Params(
            UUID userId,
            UUID revokedBy
    ) {
    }

    public void execute(Params params) {
        var now = Instant.now(clock);

        for (var session : sessionRepository.findActiveByUserId(params.userId(), now)) {
            session.revoke(params.revokedBy(), now);

            sessionRepository.save(session);

            publisher.publishAll(session.pullEvents());
        }
    }
}
