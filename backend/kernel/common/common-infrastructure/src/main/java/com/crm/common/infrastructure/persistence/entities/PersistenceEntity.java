package com.crm.common.infrastructure.persistence.entities;

/**
 * Contrato mínimo de uma entidade de persistência: ter um identificador.
 *
 * @param <ID> tipo do identificador
 */
public interface PersistenceEntity<ID> {
    ID getId();
}
