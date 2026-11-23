package com.crm.iam.application.usecases.commands;

import com.crm.common.application.annotations.UseCase;
import com.crm.common.application.events.EventPublisher;
import com.crm.common.application.ratelimit.RateLimiter;
import com.crm.iam.application.IamSettings;
import com.crm.iam.application.contracts.repositories.OneTimeTokenRepository;
import com.crm.iam.application.contracts.repositories.UserRepository;
import com.crm.iam.domain.entities.OneTimeToken;
import com.crm.iam.domain.enums.TokenPurpose;
import com.crm.iam.domain.vos.TokenSecret;
import com.crm.iam.domain.enums.UserStatus;
import com.crm.shared.domain.vos.contact.Email;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.Instant;

/**
 * Pede o link de redefinição de senha.
 *
 * <p>Responde sempre igual, exista o e-mail ou não: só uma conta ativa recebe link, e quem pede por
 * outra não fica sabendo. O e-mail inexistente também conta no limite de pedidos.
 */
@UseCase
@RequiredArgsConstructor
public class RequestPasswordReset {

    private final UserRepository userRepository;
    private final OneTimeTokenRepository tokenRepository;
    private final RateLimiter rateLimiter;
    private final EventPublisher publisher;
    private final IamSettings settings;
    private final Clock clock;

    /**
     * @param email  e-mail informado, em qualquer caixa
     * @param origin IP de origem, entregue pela web
     */
    public record Params(
            String email,
            String origin
    ) {
    }

    public void execute(Params params) {
        var now = Instant.now(clock);
        var email = Email.of(params.email());

        rateLimiter.consume(settings.passwordResetPerEmail(), email.getValue());

        var user = userRepository.findByEmail(email)
                .filter(found -> found.getStatus() == UserStatus.ACTIVE);

        if (user.isEmpty())
            return;

        for (var pending : tokenRepository.findPending(user.get().getId(), TokenPurpose.PASSWORD_RESET)) {
            pending.invalidate(now);

            tokenRepository.save(pending);
        }

        var token = OneTimeToken.issue(
                user.get().getId(),
                TokenPurpose.PASSWORD_RESET,
                TokenSecret.generate(),
                settings.passwordResetValidity(),
                now
        );

        tokenRepository.save(token);

        publisher.publishAll(token.pullEvents());
    }
}
