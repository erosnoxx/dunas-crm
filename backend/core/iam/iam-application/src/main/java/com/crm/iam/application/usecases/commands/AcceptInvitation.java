package com.crm.iam.application.usecases.commands;

import com.crm.common.application.annotations.UseCase;
import com.crm.common.application.exceptions.NotFoundException;
import com.crm.common.application.exceptions.UnprocessableEntityException;
import com.crm.iam.application.IamSettings;
import com.crm.iam.application.contracts.repositories.OneTimeTokenRepository;
import com.crm.iam.application.contracts.repositories.SessionRepository;
import com.crm.iam.application.contracts.repositories.UserRepository;
import com.crm.iam.application.contracts.services.PasswordHasher;
import com.crm.iam.application.schemas.SessionResponse;
import com.crm.iam.domain.entities.Session;
import com.crm.iam.domain.vos.DeviceLabel;
import com.crm.iam.domain.vos.Password;
import com.crm.iam.domain.vos.TokenPurpose;
import com.crm.iam.domain.vos.TokenSecret;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

/**
 * Aceita o convite: define a senha, ativa a conta e já abre a sessão.
 *
 * <p>A senha é validada antes de consumir o link, para que uma senha fraca não queime o convite. O
 * link que não existe, ou que é de outra finalidade, responde {@code link_used}, como o já usado,
 * para não revelar quais links existem.
 */
@UseCase
@RequiredArgsConstructor
public class AcceptInvitation {

    private final OneTimeTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final SessionRepository sessionRepository;
    private final PasswordHasher hasher;
    private final IamSettings settings;
    private final Clock clock;

    /**
     * @param token    segredo do link, como veio na URL
     * @param password senha escolhida, em texto
     * @param device   rótulo do aparelho
     */
    public record Params(
            String token,
            String password,
            String device
    ) {
    }

    public SessionResponse execute(Params params) {
        var now = Instant.now(clock);
        var password = Password.of(params.password());

        var token = Optional.ofNullable(params.token())
                .filter(TokenSecret::isWellFormed)
                .flatMap(raw -> tokenRepository.findByTokenHash(TokenSecret.of(raw).hash()))
                .filter(found -> found.getPurpose() == TokenPurpose.INVITATION)
                .orElseThrow(() -> new UnprocessableEntityException("link_used", "este link já foi usado ou foi substituído"));

        token.consume(now);

        if (!tokenRepository.consume(token))
            throw new UnprocessableEntityException("link_used", "este link já foi usado ou foi substituído");

        var user = userRepository.findById(token.getUserId())
                .orElseThrow(() -> new NotFoundException("user_not_found", "usuário não encontrado"));

        user.acceptInvitation(hasher.hash(password));

        userRepository.save(user);

        var secret = TokenSecret.generate();
        var session = Session.start(
                user.getId(),
                secret,
                DeviceLabel.of(params.device()),
                settings.sessionValidity(),
                now
        );

        sessionRepository.save(session);

        return new SessionResponse(secret.getValue(), session.getExpiresAt(), user.isMustChangePassword());
    }
}
