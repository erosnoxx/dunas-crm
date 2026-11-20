package com.crm.iam.application.usecases.queries;

import com.crm.common.application.annotations.UseCase;
import com.crm.iam.application.Actor;
import com.crm.iam.application.contracts.readers.SessionReader;
import com.crm.iam.application.schemas.SessionSummary;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Sessões ativas de um usuário, da mais recente para a mais antiga: as do próprio {@link Actor} ou,
 * se ele é Administrador, as de qualquer um. Só a sessão do {@link Actor} sai como corrente.
 */
@UseCase(transactional = false)
@RequiredArgsConstructor
public class ListSessions {

    private final SessionReader sessionReader;
    private final Clock clock;

    /**
     * @param actor  quem consulta
     * @param userId dono das sessões
     */
    public record Params(
            Actor actor,
            UUID userId
    ) {
    }

    public List<SessionSummary> execute(Params params) {
        var actor = params.actor();

        if (!params.userId().equals(actor.userId()))
            actor.requireAdministrator();

        return sessionReader.findActiveByUser(
                params.userId(),
                actor.sessionId(),
                Instant.now(clock)
        );
    }
}
