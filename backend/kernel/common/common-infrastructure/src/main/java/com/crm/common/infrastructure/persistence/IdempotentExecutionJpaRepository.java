package com.crm.common.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;

/**
 * Acesso JPA às execuções idempotentes. Visível só no pacote: o resto do sistema usa o
 * {@code IdempotencyGuard}.
 */
interface IdempotentExecutionJpaRepository extends JpaRepository<IdempotentExecutionEntity, String> {

    /**
     * Reivindica a chave: 1 se a linha nasceu agora, 0 se a chave já existia. Um {@code save}
     * aqui faria merge (id manual, sem versão) e sobrescreveria a execução anterior.
     */
    @Modifying
    @Query(value = """
        insert into idempotent_execution (id, request_fingerprint, status, created_at)
        values (:id, :requestFingerprint, 'IN_PROGRESS', :createdAt)
        on conflict (id) do nothing
        """, nativeQuery = true)
    int insertIfAbsent(String id, String requestFingerprint, Instant createdAt);

    /**
     * A única forma segura de reabrir uma execução {@code FAILED}: um {@code UPDATE}
     * condicionado ao estado atual, atômico no nível do banco. Se duas tentativas concorrentes
     * chegarem aqui ao mesmo tempo para a mesma chave, só uma altera a linha (retorna 1); a
     * outra recebe 0 e trata como conflito — nunca as duas acham que "ganharam" a retentativa.
     */
    @Modifying
    @Query("""
        update IdempotentExecutionEntity e
        set e.status = com.crm.common.infrastructure.persistence.IdempotentExecutionStatus.IN_PROGRESS,
            e.failureReason = null,
            e.completedAt = null
        where e.id = :key
        and e.status = com.crm.common.infrastructure.persistence.IdempotentExecutionStatus.FAILED
        """)
    int beginRetry(String key);
}
