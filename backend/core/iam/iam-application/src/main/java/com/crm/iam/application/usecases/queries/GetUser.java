package com.crm.iam.application.usecases.queries;

import com.crm.common.application.annotations.UseCase;
import com.crm.common.application.exceptions.NotFoundException;
import com.crm.iam.application.Actor;
import com.crm.iam.application.contracts.readers.SessionReader;
import com.crm.iam.application.contracts.readers.UserReader;
import com.crm.iam.application.schemas.UserDetail;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

/** Detalhe de um usuário: o resumo e as sessões ativas dele. Só o Administrador consulta (ADR-5). */
@UseCase(transactional = false)
@RequiredArgsConstructor
public class GetUser {

    private final UserReader userReader;
    private final SessionReader sessionReader;
    private final Clock clock;

    /**
     * @param actor  quem consulta
     * @param userId usuário consultado
     */
    public record Params(
            Actor actor,
            UUID userId
    ) {
    }

    public UserDetail execute(Params params) {
        var actor = params.actor();

        actor.requireAdministrator();

        var summary = userReader.findSummary(params.userId())
                .orElseThrow(() -> new NotFoundException("user_not_found", "usuário não encontrado"));

        var sessions = sessionReader.findActiveByUser(
                params.userId(),
                actor.sessionId(),
                Instant.now(clock)
        );

        return new UserDetail(summary, sessions);
    }
}
