package com.crm.iam.application.contracts.repositories;

import com.crm.common.application.repository.Repository;
import com.crm.iam.domain.entities.Session;
import com.crm.iam.domain.vos.TokenHash;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Persistência do aggregate {@link Session}. */
public interface SessionRepository extends Repository<Session, SessionCriteria> {

    Optional<Session> findByTokenHash(TokenHash tokenHash);

    /** Sessões do usuário que não foram revogadas e ainda não expiraram em {@code now}. */
    List<Session> findActiveByUserId(
            UUID userId,
            Instant now
    );
}
