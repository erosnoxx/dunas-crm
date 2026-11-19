package com.crm.iam.application.usecases.commands;

import com.crm.common.application.annotations.UseCase;
import com.crm.common.application.cache.Cache;
import com.crm.common.application.exceptions.ConcurrentUpdateException;
import com.crm.common.application.exceptions.UnauthorizedException;
import com.crm.iam.application.Actor;
import com.crm.iam.application.IamSettings;
import com.crm.iam.application.contracts.readers.SessionReader;
import com.crm.iam.application.contracts.repositories.SessionRepository;
import com.crm.iam.application.schemas.SessionPrincipal;
import com.crm.iam.domain.vos.TokenSecret;
import com.crm.iam.domain.vos.UserStatus;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.Instant;

/**
 * Autentica o token opaco de cada requisição.
 *
 * <p>Lê a sessão pelo {@link Cache}, que por contrato nunca propaga falha: com o Redis fora do ar o
 * carregamento cai no {@link SessionReader} e a autenticação continua certa, só mais lenta. Por
 * isso não há {@code try/catch} aqui. O cache nunca decide: o banco é a fonte de verdade, e a
 * invalidação principal é por evento (a chave é removida pelo listener de {@code SessionRevoked}).
 *
 * <p>Só carrega e grava a {@code Session} quando a renovação é devida; a decisão final é de
 * {@code Session.renewIfDue}. A renovação é de conveniência: conflito de versão ao gravá-la
 * (outra requisição renovou antes) não derruba a autenticação. Por isso o use case não abre
 * transação: o conflito marcaria a transação externa como só-rollback e o commit falharia mesmo
 * com a exception capturada; a única escrita é atômica por si.
 *
 * <p>Token malformado (lixo no cookie ou no cabeçalho) é {@code session_invalid}, como o desconhecido.
 */
@UseCase(transactional = false)
@RequiredArgsConstructor
public class AuthenticateSession {

    /** Prefixo da chave de cache da sessão, usado também pelos listeners que a invalidam. */
    public static final String CACHE_KEY_PREFIX = "iam:session:";

    private final SessionReader sessionReader;
    private final SessionRepository sessionRepository;
    private final Cache cache;
    private final IamSettings settings;
    private final Clock clock;

    /**
     * @param actor              quem está autenticado
     * @param mustChangePassword se a conta só pode trocar a senha ou sair
     */
    public record AuthenticatedSession(
            Actor actor,
            boolean mustChangePassword
    ) {
    }

    public AuthenticatedSession execute(String token) {
        var now = Instant.now(clock);

        if (!TokenSecret.isWellFormed(token))
            throw sessionInvalid();

        var tokenHash = TokenSecret.of(token).hash();

        var principal = cache.getOrLoad(
                CACHE_KEY_PREFIX + tokenHash.getValue(),
                SessionPrincipal.class,
                settings.sessionCacheTtl(),
                () -> sessionReader.findPrincipal(tokenHash).orElse(null)
        );

        if (principal == null || !isValid(principal, now))
            throw sessionInvalid();

        if (isRenewalDue(principal, now))
            renew(principal, now);

        return new AuthenticatedSession(
                new Actor(principal.userId(), principal.role(), principal.sessionId()),
                principal.mustChangePassword()
        );
    }

    private void renew(
            SessionPrincipal principal,
            Instant now
    ) {
        var session = sessionRepository.findById(principal.sessionId())
                .orElseThrow(AuthenticateSession::sessionInvalid);

        // o cache pode estar até um TTL atrás do banco: sessão revogada lá é 401, não 422 do domínio
        if (!session.isActiveAt(now))
            throw sessionInvalid();

        if (!session.renewIfDue(now, settings.sessionValidity(), settings.sessionRenewInterval()))
            return;

        try {
            sessionRepository.save(session);
        }
        catch (ConcurrentUpdateException e) {
            // a renovação é de conveniência: se outra requisição renovou a mesma sessão primeiro,
            // o resultado é o que se queria, e a autenticação desta não pode falhar por isso
        }
    }

    private boolean isValid(
            SessionPrincipal principal,
            Instant now
    ) {
        return principal.revokedAt() == null
                && now.isBefore(principal.expiresAt())
                && principal.userStatus() == UserStatus.ACTIVE;
    }

    /** Pré-filtro para não ir ao banco a cada requisição; a regra de verdade é do aggregate. */
    private boolean isRenewalDue(
            SessionPrincipal principal,
            Instant now
    ) {
        return !now.isBefore(principal.lastUsedAt().plus(settings.sessionRenewInterval()));
    }

    private static UnauthorizedException sessionInvalid() {
        return new UnauthorizedException("session_invalid", "sua sessão não vale mais; entre de novo");
    }
}
