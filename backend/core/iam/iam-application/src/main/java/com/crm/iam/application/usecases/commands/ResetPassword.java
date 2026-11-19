package com.crm.iam.application.usecases.commands;

import com.crm.common.application.annotations.UseCase;
import com.crm.common.application.events.EventPublisher;
import com.crm.common.application.exceptions.NotFoundException;
import com.crm.common.application.exceptions.UnprocessableEntityException;
import com.crm.iam.application.contracts.repositories.OneTimeTokenRepository;
import com.crm.iam.application.contracts.repositories.SessionRepository;
import com.crm.iam.application.contracts.repositories.UserRepository;
import com.crm.iam.application.contracts.services.PasswordHasher;
import com.crm.iam.domain.vos.Password;
import com.crm.iam.domain.vos.TokenPurpose;
import com.crm.iam.domain.vos.TokenSecret;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

/**
 * Redefine a senha pelo link recebido por e-mail.
 *
 * <p>Revoga todas as sessões do usuário, porque quem esqueceu a senha pode estar com a conta
 * comprometida, e não abre sessão: o usuário entra de novo pelo login. A senha é validada antes de
 * consumir o link, para que uma senha fraca não o queime.
 */
@UseCase
@RequiredArgsConstructor
public class ResetPassword {

    private final OneTimeTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final SessionRepository sessionRepository;
    private final PasswordHasher hasher;
    private final EventPublisher publisher;
    private final Clock clock;

    /**
     * @param token    segredo do link, como veio na URL
     * @param password nova senha, em texto
     */
    public record Params(
            String token,
            String password
    ) {
    }

    public void execute(Params params) {
        var now = Instant.now(clock);
        var password = Password.of(params.password());

        var token = Optional.ofNullable(params.token())
                .filter(TokenSecret::isWellFormed)
                .flatMap(raw -> tokenRepository.findByTokenHash(TokenSecret.of(raw).hash()))
                .filter(found -> found.getPurpose() == TokenPurpose.PASSWORD_RESET)
                .orElseThrow(() -> new UnprocessableEntityException("link_used", "este link já foi usado ou foi substituído"));

        token.consume(now);

        if (!tokenRepository.consume(token))
            throw new UnprocessableEntityException("link_used", "este link já foi usado ou foi substituído");

        var user = userRepository.findById(token.getUserId())
                .orElseThrow(() -> new NotFoundException("user_not_found", "usuário não encontrado"));

        user.resetPassword(hasher.hash(password));

        userRepository.save(user);

        for (var session : sessionRepository.findActiveByUserId(user.getId(), now)) {
            session.revoke(user.getId(), now);

            sessionRepository.save(session);

            publisher.publishAll(session.pullEvents());
        }
    }
}
