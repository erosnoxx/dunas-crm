package com.crm.iam.application.contracts.readers;

import com.crm.iam.application.schemas.SessionPrincipal;
import com.crm.iam.application.schemas.SessionSummary;
import com.crm.iam.domain.vos.TokenHash;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Leitura de sessões em projeção, sem montar o aggregate. */
public interface SessionReader {

    /** Sessões ativas do usuário em {@code now}; a de id {@code currentSessionId} sai marcada. */
    List<SessionSummary> findActiveByUser(
            UUID userId,
            UUID currentSessionId,
            Instant now
    );

    /** Sessão e dados do usuário numa leitura só, o que a autenticação de cada requisição usa. */
    Optional<SessionPrincipal> findPrincipal(TokenHash tokenHash);
}
