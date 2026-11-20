package com.crm.iam.infrastructure.persistence;

import com.crm.common.infrastructure.persistence.repositories.RepositoryImpl;
import com.crm.iam.application.contracts.repositories.SessionCriteria;
import com.crm.iam.application.contracts.repositories.SessionRepository;
import com.crm.iam.domain.entities.Session;
import jakarta.persistence.EntityManager;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Repository de {@link Session} sobre JPA. */
public class JpaSessionRepositoryImpl
        extends RepositoryImpl<Session, SessionCriteria, SessionEntity, SessionJpaRepository>
        implements SessionRepository {

    public JpaSessionRepositoryImpl(
            SessionJpaRepository jpaRepository,
            EntityManager em,
            SessionMapper mapper
    ) {
        super(
                jpaRepository,
                em,
                mapper,
                SessionEntity.class
        );
    }

    @Override
    public List<Session> findActiveByUserId(
            UUID userId,
            Instant now
    ) {
        return jpaRepository.findByUserIdAndRevokedAtIsNullAndExpiresAtAfter(userId, now).stream()
                .map(mapper::toDomain)
                .toList();
    }
}
