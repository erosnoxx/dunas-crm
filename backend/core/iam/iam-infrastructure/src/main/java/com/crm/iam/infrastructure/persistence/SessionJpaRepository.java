package com.crm.iam.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Spring Data de {@link SessionEntity}; só a infraestrutura o enxerga. */
public interface SessionJpaRepository extends JpaRepository<SessionEntity, UUID> {

    Optional<SessionEntity> findByTokenHash(String tokenHash);

    List<SessionEntity> findByUserIdAndRevokedAtIsNullAndExpiresAtAfter(
            UUID userId,
            Instant now
    );
}
