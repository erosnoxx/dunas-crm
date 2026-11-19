package com.crm.common.infrastructure.persistence.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

/**
 * Base das entidades JPA: id, datas de criação e atualização (preenchidas por
 * {@link PersistenceTimestampsListener}) e versão para lock otimista.
 *
 * @param <ID> tipo do identificador
 */
@MappedSuperclass
@EntityListeners(PersistenceTimestampsListener.class)
@Getter @Setter
public abstract class JpaEntity<ID> implements PersistenceEntity<ID> {
    @Id
    private ID id;

    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;
    @Column
    private OffsetDateTime updatedAt;

    /**
     * Lock otimista (docs/architecture.md §8): o UPDATE confere a versão lida e a incrementa.
     * Nula enquanto a linha não existe; é por ela que o Spring Data trata a entity como nova.
     */
    @Version
    private Long version;
}
