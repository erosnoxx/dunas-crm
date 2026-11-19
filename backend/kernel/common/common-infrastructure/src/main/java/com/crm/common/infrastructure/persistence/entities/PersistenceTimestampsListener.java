package com.crm.common.infrastructure.persistence.entities;

import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;

import java.time.Clock;
import java.time.OffsetDateTime;

/**
 * Preenche {@code createdAt}/{@code updatedAt} de todo {@link JpaEntity} a partir do
 * {@link Clock} do sistema.
 *
 * <p>É um listener separado, e não {@code @PrePersist} na própria entity, porque callback de
 * entity não recebe injeção. O Hibernate instancia este listener pelo contêiner do Spring
 * (o Spring Boot configura o {@code SpringBeanContainer}), que injeta o {@code Clock} pelo
 * construtor. Se essa integração faltar, o Hibernate não acha construtor sem argumento e a
 * aplicação não sobe — falha alta, nunca uma volta silenciosa ao relógio da máquina.
 */
public class PersistenceTimestampsListener {

    private final Clock clock;

    public PersistenceTimestampsListener(Clock clock) {
        this.clock = clock;
    }

    @PrePersist
    void prePersist(JpaEntity<?> entity) {
        if (entity.getCreatedAt() != null)
            return;

        entity.setCreatedAt(OffsetDateTime.now(clock));
    }

    @PreUpdate
    void preUpdate(JpaEntity<?> entity) {
        entity.setUpdatedAt(OffsetDateTime.now(clock));
    }
}
