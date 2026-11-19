package com.crm.common.infrastructure.persistence.repositories;

import com.crm.common.domain.entities.DomainEntity;
import com.crm.common.infrastructure.persistence.entities.PersistenceEntity;

/**
 * Converte entre a entidade de domínio e a entidade de persistência do mesmo conceito.
 *
 * <p>É o que mantém o domínio livre de anotação JPA.
 *
 * @param <D> entidade de domínio
 * @param <P> entidade de persistência
 */
public interface EntityMapper<D extends DomainEntity, P extends PersistenceEntity<?>> {
    D toDomain(P persistence);

    P toPersistence(D domain);
}
