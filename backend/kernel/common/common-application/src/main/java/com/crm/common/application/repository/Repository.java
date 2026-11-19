package com.crm.common.application.repository;



import com.crm.common.application.pages.Page;
import com.crm.common.application.pages.Pageable;
import com.crm.common.domain.entities.AggregateRoot;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Contrato de persistência de um aggregate root, escrito na linguagem do domínio.
 *
 * <p>Implementado na infrastructure; a application só conhece esta interface.
 *
 * @param <A> aggregate root persistido
 * @param <C> tipo do critério de busca do aggregate
 */
public interface Repository<A extends AggregateRoot, C extends Criteria> {

    A save(A entity);
    List<A> saveBatch(Iterable<A> entities);
    A saveAndFlush(A entity);

    void flush();

    void delete(A entity);
    void deleteAll(Iterable<A> entities);
    void deleteAll();

    Optional<A> findById(UUID id);
    boolean existsById(UUID id);

    long count();
    long count(C criteria);

    List<A> findAll();
    List<A> findAll(C criteria);
    Page<A> findAll(Pageable pageable);
    Page<A> findAll(C criteria, Pageable pageable);
}