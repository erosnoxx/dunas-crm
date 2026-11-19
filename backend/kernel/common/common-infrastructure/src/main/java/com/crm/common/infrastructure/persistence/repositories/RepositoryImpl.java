package com.crm.common.infrastructure.persistence.repositories;

import com.crm.common.application.exceptions.ConcurrentUpdateException;
import com.crm.common.application.exceptions.ConflictException;
import com.crm.common.application.pages.Page;
import com.crm.common.application.pages.Pageable;
import com.crm.common.application.repository.Criteria;
import com.crm.common.application.repository.Repository;
import com.crm.common.domain.entities.AggregateRoot;
import com.crm.common.infrastructure.persistence.entities.PersistenceEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Base genérica de repository. Traduz entre aggregate e persistence entity, e traduz
 * violação de constraint em {@link ConflictException} e versão velha em
 * {@link ConcurrentUpdateException}: nenhuma exception de terceiro vaza crua desta camada.
 *
 * <p><b>O save grava na hora (flush).</b> É no flush que a constraint e a versão são conferidas;
 * deixado para o commit, o erro sairia fora daqui, cru, como 500. E a instância devolvida já
 * tem a versão nova, que é o que outro save na mesma requisição precisa (docs/architecture.md §8).
 *
 * <p><b>Esta classe não publica domain event.</b> O aggregate registra o evento e a
 * camada de application publica depois que o save foi confirmado (ver docs/architecture.md
 * §5.1). No projeto anterior a publicação acontecia aqui dentro de {@code save}; foi uma
 * mudança deliberada, não um esquecimento — o repository não deve decidir quando um evento
 * se torna observável.
 */
public class RepositoryImpl<
        A extends AggregateRoot, C extends Criteria,
        P extends PersistenceEntity<UUID>, R extends JpaRepository<P, UUID>>
        implements Repository<A, C> {

    protected final R jpaRepository;
    protected final EntityManager em;
    protected final EntityMapper<A, P> mapper;
    protected final Class<P> entityClass;

    protected RepositoryImpl(
            R jpaRepository,
            EntityManager em,
            EntityMapper<A, P> mapper,
            Class<P> entityClass
    ) {
        this.jpaRepository = jpaRepository;
        this.em = em;
        this.mapper = mapper;
        this.entityClass = entityClass;
    }

    @Override
    public A save(A entity) {
        var saved = translatingConflicts(() -> jpaRepository.saveAndFlush(mapper.toPersistence(entity)));

        return mapper.toDomain(saved);
    }

    @Override
    public List<A> saveBatch(Iterable<A> entities) {
        List<P> toSave = new ArrayList<>();
        entities.forEach(entity -> toSave.add(mapper.toPersistence(entity)));

        var saved = translatingConflicts(() -> jpaRepository.saveAllAndFlush(toSave));

        return saved.stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public A saveAndFlush(A entity) {
        return save(entity);
    }

    @Override
    public void flush() {
        jpaRepository.flush();
    }

    @Override
    public void delete(A entity) {
        jpaRepository.delete(mapper.toPersistence(entity));
    }

    @Override
    public void deleteAll(Iterable<A> entities) {
        List<P> toDelete = new ArrayList<>();
        entities.forEach(entity -> toDelete.add(mapper.toPersistence(entity)));

        jpaRepository.deleteAll(toDelete);
    }

    @Override
    public void deleteAll() {
        jpaRepository.deleteAll();
    }

    @Override
    public Optional<A> findById(UUID id) {
        return jpaRepository.findById(id)
                .map(mapper::toDomain);
    }

    @Override
    public boolean existsById(UUID id) {
        return jpaRepository.existsById(id);
    }

    @Override
    public long count() {
        return jpaRepository.count();
    }

    @Override
    public long count(C criteria) {
        var cb = em.getCriteriaBuilder();
        var query = cb.createQuery(Long.class);
        var root = query.from(entityClass);

        query.select(cb.count(root))
                .where(CriteriaUtils.buildPredicates(criteria, root, cb));

        return em.createQuery(query).getSingleResult();
    }

    @Override
    public List<A> findAll() {
        return jpaRepository.findAll().stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<A> findAll(C criteria) {
        return queryPersistence(criteria, null).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public Page<A> findAll(Pageable pageable) {
        var springPage = jpaRepository.findAll(PageableAdapter.toSpring(pageable));

        return PageableAdapter.toDomain(springPage.map(mapper::toDomain));
    }

    @Override
    public Page<A> findAll(C criteria, Pageable pageable) {
        var content = queryPersistence(criteria, pageable).stream()
                .map(mapper::toDomain)
                .toList();

        var total = count(criteria);

        return new Page<>(
                content,
                pageable.page(),
                pageable.size(),
                total,
                totalPages(total, pageable.size())
        );
    }

    protected <T> T translatingConflicts(Supplier<T> operation) {
        try {
            return operation.get();
        }

        catch (OptimisticLockingFailureException e) {
            throw new ConcurrentUpdateException(e);
        }

        catch (DataIntegrityViolationException e) {
            throw new ConflictException("operação conflita com um registro já existente", e);
        }
    }

    private List<P> queryPersistence(C criteria, Pageable pageable) {
        var cb = em.getCriteriaBuilder();
        var query = cb.createQuery(entityClass);
        var root = query.from(entityClass);

        query.where(CriteriaUtils.buildPredicates(criteria, root, cb));

        if (pageable == null)
            return em.createQuery(query).getResultList();

        applySorting(query, root, cb, pageable);

        var typedQuery = em.createQuery(query);
        typedQuery.setFirstResult(pageable.getOffset());
        typedQuery.setMaxResults(pageable.getPageSize());

        return typedQuery.getResultList();
    }

    private void applySorting(
            CriteriaQuery<?> query,
            Root<?> root,
            CriteriaBuilder cb,
            Pageable pageable
    ) {
        if (pageable.sortBy() == null)
            return;

        var path = root.get(pageable.sortBy());

        if ("DESC".equalsIgnoreCase(pageable.sortDirection())) {
            query.orderBy(cb.desc(path));
            return;
        }

        query.orderBy(cb.asc(path));
    }

    private int totalPages(long total, int size) {
        if (size <= 0)
            return 0;

        return (int) Math.ceil((double) total / size);
    }
}
