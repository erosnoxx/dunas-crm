package com.crm.iam.application.contracts.repositories;

import com.crm.common.application.repository.Repository;
import com.crm.iam.domain.entities.OneTimeToken;
import com.crm.iam.domain.vos.TokenHash;
import com.crm.iam.domain.vos.TokenPurpose;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Persistência do aggregate {@link OneTimeToken}. */
public interface OneTimeTokenRepository extends Repository<OneTimeToken, OneTimeTokenCriteria> {

    Optional<OneTimeToken> findByTokenHash(TokenHash tokenHash);

    /** Tokens do usuário, para a finalidade dada, que ainda não foram usados nem invalidados. */
    List<OneTimeToken> findPending(
            UUID userId,
            TokenPurpose purpose
    );

    /**
     * Grava o uso do token só se ele ainda não foi usado nem invalidado (update condicional).
     *
     * <p>É o que decide a corrida entre duas requisições com o mesmo link: só uma recebe
     * {@code true}.
     *
     * <p>Não salve esta instância depois do consume: a linha já está em uma versão mais nova e o
     * save falharia por conflito de versão.
     *
     * @return {@code false} se outra requisição consumiu o token antes
     */
    boolean consume(OneTimeToken token);
}
