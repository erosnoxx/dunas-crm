package com.crm.common.infrastructure.outbox.persistence;

import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Acesso JPA ao outbox. Visível só no pacote: o resto do sistema usa {@code OutboxReader} e
 * {@code OutboxWriter}.
 */
interface OutboxEventJpaRepository extends JpaRepository<OutboxEventEntity, UUID> {

    /**
     * SELECT ... FOR UPDATE SKIP LOCKED.
     *
     * <p>{@code lock.timeout = -2} é o valor que o Hibernate interpreta como SKIP LOCKED — é o
     * que permite várias instâncias despacharem em paralelo sem pegar a mesma linha e sem
     * ninguém ficar bloqueado esperando.
     *
     * <p><b>Só funciona dentro de uma transação.</b> Chamado fora de uma, nenhum lock é
     * adquirido e o SKIP LOCKED não tem efeito nenhum — ver {@code OutboxDispatcherJob}.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "-2"))
    @Query("""
        select e from OutboxEventEntity e
        where e.status = com.crm.common.infrastructure.outbox.persistence.OutboxEventStatus.PENDING
        and (e.nextRetryAt is null or e.nextRetryAt <= :now)
        order by e.occurredOn asc
        """)
    List<OutboxEventEntity> findDispatchable(Instant now, Pageable pageable);

    @Modifying
    @Query("""
        delete from OutboxEventEntity e
        where e.status = com.crm.common.infrastructure.outbox.persistence.OutboxEventStatus.PROCESSED
        and e.processedAt < :cutoff
        """)
    void deleteProcessedBefore(Instant cutoff);
}
