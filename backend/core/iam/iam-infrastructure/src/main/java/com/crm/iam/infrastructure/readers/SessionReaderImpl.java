package com.crm.iam.infrastructure.readers;

import com.crm.iam.application.contracts.readers.SessionReader;
import com.crm.iam.application.schemas.SessionPrincipal;
import com.crm.iam.application.schemas.SessionSummary;
import com.crm.iam.domain.vos.TokenHash;
import jakarta.persistence.EntityManager;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Leitura de sessões em projeção. {@link #findPrincipal} junta sessão e usuário numa consulta só:
 * é o que a autenticação de cada requisição paga.
 */
public class SessionReaderImpl implements SessionReader {

    private final EntityManager em;

    public SessionReaderImpl(EntityManager em) {
        this.em = em;
    }

    @Override
    public List<SessionSummary> findActiveByUser(
            UUID userId,
            UUID currentSessionId,
            Instant now
    ) {
        return em.createQuery("""
                        select new com.crm.iam.application.schemas.SessionSummary(
                            s.id, s.device, s.startedAt, s.lastUsedAt,
                            case when s.id = :currentSessionId then true else false end
                        )
                        from SessionEntity s
                        where s.userId = :userId and s.revokedAt is null and s.expiresAt > :now
                        order by s.lastUsedAt desc, s.id
                        """, SessionSummary.class)
                .setParameter("userId", userId)
                .setParameter("currentSessionId", currentSessionId)
                .setParameter("now", now)
                .getResultList();
    }

    @Override
    public Optional<SessionPrincipal> findPrincipal(TokenHash tokenHash) {
        return em.createQuery("""
                        select new com.crm.iam.application.schemas.SessionPrincipal(
                            s.id, u.id, u.role, u.status, u.mustChangePassword,
                            s.lastUsedAt, s.expiresAt, s.revokedAt
                        )
                        from SessionEntity s, UserEntity u
                        where u.id = s.userId and s.tokenHash = :tokenHash
                        """, SessionPrincipal.class)
                .setParameter("tokenHash", tokenHash.getValue())
                .getResultList().stream()
                .findFirst();
    }
}
