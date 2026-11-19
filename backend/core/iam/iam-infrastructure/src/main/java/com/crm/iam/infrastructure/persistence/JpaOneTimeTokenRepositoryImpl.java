package com.crm.iam.infrastructure.persistence;

import com.crm.common.application.exceptions.InternalServerErrorException;
import com.crm.common.infrastructure.persistence.repositories.RepositoryImpl;
import com.crm.iam.application.contracts.repositories.OneTimeTokenCriteria;
import com.crm.iam.application.contracts.repositories.OneTimeTokenRepository;
import com.crm.iam.domain.entities.OneTimeToken;
import com.crm.iam.domain.vos.TokenHash;
import com.crm.iam.domain.vos.TokenPurpose;
import jakarta.persistence.EntityManager;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository de {@link OneTimeToken} sobre JPA. O consumo é um update condicional, e não um
 * save: é ele que decide qual de duas requisições simultâneas usa o link.
 */
public class JpaOneTimeTokenRepositoryImpl
        extends RepositoryImpl<OneTimeToken, OneTimeTokenCriteria, OneTimeTokenEntity, OneTimeTokenJpaRepository>
        implements OneTimeTokenRepository {

    public JpaOneTimeTokenRepositoryImpl(
            OneTimeTokenJpaRepository jpaRepository,
            EntityManager em,
            OneTimeTokenMapper mapper
    ) {
        super(jpaRepository, em, mapper, OneTimeTokenEntity.class);
    }

    @Override
    public Optional<OneTimeToken> findByTokenHash(TokenHash tokenHash) {
        return jpaRepository.findByTokenHash(tokenHash.getValue())
                .map(mapper::toDomain);
    }

    @Override
    public List<OneTimeToken> findPending(
            UUID userId,
            TokenPurpose purpose
    ) {
        return jpaRepository.findByUserIdAndPurposeAndUsedAtIsNullAndInvalidatedAtIsNull(userId, purpose).stream()
                .map(mapper::toDomain)
                .toList();
    }

    /**
     * A hora do uso é a que o aggregate já guarda: quem chama marca o token com
     * {@code consume(now)} (e confere a validade) antes de gravar. Depois do consume, a instância
     * não deve ser salva: a linha já está em uma versão mais nova e o save falharia por conflito
     * de versão.
     */
    @Override
    public boolean consume(OneTimeToken token) {
        var usedAt = token.getUsedAt()
                .orElseThrow(() -> new InternalServerErrorException("o token precisa estar marcado como usado antes de gravar o consumo"));

        return jpaRepository.consume(token.getId(), usedAt) == 1;
    }
}
