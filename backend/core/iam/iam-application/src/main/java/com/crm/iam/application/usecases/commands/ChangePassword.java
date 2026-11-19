package com.crm.iam.application.usecases.commands;

import com.crm.common.application.annotations.UseCase;
import com.crm.common.application.cache.Cache;
import com.crm.common.application.events.EventPublisher;
import com.crm.common.application.exceptions.NotFoundException;
import com.crm.common.application.exceptions.UnprocessableEntityException;
import com.crm.common.application.ratelimit.RateLimiter;
import com.crm.iam.application.Actor;
import com.crm.iam.application.IamSettings;
import com.crm.iam.application.contracts.repositories.SessionRepository;
import com.crm.iam.application.contracts.repositories.UserRepository;
import com.crm.iam.application.contracts.services.PasswordHasher;
import com.crm.iam.domain.vos.Password;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.Instant;

/**
 * Troca a senha do próprio usuário, conferindo a senha atual.
 *
 * <p>A conferência da senha atual conta no limite de login do e-mail do usuário, como o login:
 * sem isso, uma sessão roubada serviria para adivinhar a senha sem freio. As outras sessões do
 * usuário são revogadas e a do {@link Actor} continua. A senha atual fora da regra de formato é
 * senha errada ({@code wrong_current_password}); só a nova senha responde {@code invalid_password}.
 *
 * <p>A invalidação do cache por evento é o mecanismo principal (architecture §6.2), mas nenhum
 * domain event cobre a troca de senha: por isso este fluxo tira da cache a sessão atual, que guarda
 * {@code mustChangePassword}, direto, logo depois de salvar. O TTL de 60 segundos cobre a corrida
 * residual de uma requisição que repovoe o cache antes do commit.
 */
@UseCase
@RequiredArgsConstructor
public class ChangePassword {

    private final UserRepository userRepository;
    private final SessionRepository sessionRepository;
    private final PasswordHasher hasher;
    private final RateLimiter rateLimiter;
    private final EventPublisher publisher;
    private final Cache cache;
    private final IamSettings settings;
    private final Clock clock;

    /**
     * @param actor           quem troca a senha e a sessão que usa
     * @param currentPassword senha atual, em texto
     * @param newPassword     nova senha, em texto
     */
    public record Params(
            Actor actor,
            String currentPassword,
            String newPassword
    ) {
    }

    public void execute(Params params) {
        var now = Instant.now(clock);
        var actor = params.actor();

        var user = userRepository.findById(actor.userId())
                .orElseThrow(() -> new NotFoundException("user_not_found", "usuário não encontrado"));

        rateLimiter.consume(settings.loginPerEmail(), user.getEmail().getValue());

        var next = Password.of(params.newPassword());

        var currentHash = user.getPasswordHash()
                .orElseThrow(() -> new UnprocessableEntityException("user_not_active", "este usuário não está ativo"));

        // senha atual fora da regra nunca bate com o hash guardado: é senha errada, e não 400
        var matches = Password.isValid(params.currentPassword())
                && hasher.matches(Password.of(params.currentPassword()), currentHash);

        if (!matches)
            throw new UnprocessableEntityException("wrong_current_password", "a senha atual está incorreta");

        rateLimiter.refund(settings.loginPerEmail(), user.getEmail().getValue());

        user.changePassword(hasher.hash(next));

        userRepository.save(user);

        for (var session : sessionRepository.findActiveByUserId(user.getId(), now)) {
            if (session.getId().equals(actor.sessionId())) {
                cache.evict(AuthenticateSession.CACHE_KEY_PREFIX + session.getTokenHash().getValue());

                continue;
            }

            session.revoke(user.getId(), now);

            sessionRepository.save(session);

            publisher.publishAll(session.pullEvents());
        }
    }
}
