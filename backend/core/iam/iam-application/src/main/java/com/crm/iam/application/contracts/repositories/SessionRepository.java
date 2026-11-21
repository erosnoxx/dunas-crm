package com.crm.iam.application.contracts.repositories;

import com.crm.common.application.repository.Repository;
import com.crm.iam.domain.entities.Session;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Persistência do aggregate {@link Session}. */
public interface SessionRepository extends Repository<Session, SessionCriteria> {

    /** Sessões do usuário que não foram revogadas e ainda não expiraram em {@code now}. */
    List<Session> findActiveByUserId(
            UUID userId,
            Instant now
    );
}
