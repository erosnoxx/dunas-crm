package com.crm.iam.infrastructure.persistence;

import com.crm.iam.domain.vos.TokenPurpose;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Spring Data de {@link OneTimeTokenEntity}; só a infraestrutura o enxerga. */
public interface OneTimeTokenJpaRepository extends JpaRepository<OneTimeTokenEntity, UUID> {

    Optional<OneTimeTokenEntity> findByTokenHash(String tokenHash);

    List<OneTimeTokenEntity> findByUserIdAndPurposeAndUsedAtIsNullAndInvalidatedAtIsNull(
            UUID userId,
            TokenPurpose purpose
    );

    /**
     * Update condicional: é o banco que decide a corrida entre duas requisições com o mesmo link,
     * porque o segundo UPDATE reavalia o WHERE depois que o primeiro confirma. Devolve as linhas
     * afetadas (1 se este chamador venceu, 0 se não).
     */
    @Transactional
    @Modifying
    @Query("""
            update OneTimeTokenEntity t
               set t.usedAt = :now, t.version = t.version + 1
             where t.id = :id and t.usedAt is null and t.invalidatedAt is null
            """)
    int consume(
            @Param("id") UUID id,
            @Param("now") Instant now
    );
}
