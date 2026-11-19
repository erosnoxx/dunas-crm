package com.crm.iam.application.usecases.commands;

import com.crm.common.application.annotations.UseCase;
import com.crm.common.application.exceptions.UnauthorizedException;
import com.crm.common.application.ratelimit.RateLimiter;
import com.crm.iam.application.IamSettings;
import com.crm.iam.application.contracts.repositories.SessionRepository;
import com.crm.iam.application.contracts.repositories.UserRepository;
import com.crm.iam.application.contracts.services.PasswordHasher;
import com.crm.iam.application.schemas.SessionResponse;
import com.crm.iam.domain.entities.Session;
import com.crm.iam.domain.entities.User;
import com.crm.iam.domain.vos.DeviceLabel;
import com.crm.iam.domain.vos.Password;
import com.crm.iam.domain.vos.PasswordHash;
import com.crm.iam.domain.vos.TokenSecret;
import com.crm.iam.domain.vos.UserStatus;
import com.crm.shared.domain.vos.contact.Email;

import java.time.Clock;
import java.time.Instant;

/**
 * Abre uma sessão a partir de e-mail e senha.
 *
 * <p>Qualquer falha de credencial (e-mail inexistente, senha errada, conta convidada ou
 * desativada) é a mesma {@code invalid_credentials}, e o {@link PasswordHasher} roda sempre: contra
 * o hash do usuário ou contra um hash fixo, para que o tempo de resposta não revele quem tem conta.
 *
 * <p>O hash fixo é calculado pelo próprio hasher na construção, e não escrito como constante, para
 * acompanhar os parâmetros de custo configurados em cada ambiente.
 *
 * <p>Os limites de tentativa são consumidos antes de conferir a credencial e devolvidos só no acerto.
 */
@UseCase
public class Login {

    private final UserRepository userRepository;
    private final SessionRepository sessionRepository;
    private final PasswordHasher hasher;
    private final RateLimiter rateLimiter;
    private final IamSettings settings;
    private final Clock clock;
    private final PasswordHash dummyHash;

    public Login(
            UserRepository userRepository,
            SessionRepository sessionRepository,
            PasswordHasher hasher,
            RateLimiter rateLimiter,
            IamSettings settings,
            Clock clock
    ) {
        this.userRepository = userRepository;
        this.sessionRepository = sessionRepository;
        this.hasher = hasher;
        this.rateLimiter = rateLimiter;
        this.settings = settings;
        this.clock = clock;
        this.dummyHash = hasher.hash(Password.of("Dummy#Password1"));
    }

    /**
     * @param email    e-mail informado, em qualquer caixa
     * @param password senha em texto
     * @param device   rótulo do aparelho
     * @param origin   IP de origem, entregue pela web
     */
    public record Params(
            String email,
            String password,
            String device,
            String origin
    ) {
    }

    public SessionResponse execute(Params params) {
        var now = Instant.now(clock);
        var email = Email.of(params.email());
        var password = Password.of(params.password());

        rateLimiter.consume(settings.loginPerEmail(), email.getValue());
        rateLimiter.consume(settings.loginPerOrigin(), params.origin());

        var user = userRepository.findByEmail(email)
                .filter(found -> found.getStatus() == UserStatus.ACTIVE);

        var hash = user.flatMap(User::getPasswordHash).orElse(dummyHash);
        var matches = hasher.matches(password, hash);

        if (user.isEmpty() || !matches)
            throw new UnauthorizedException("invalid_credentials", "e-mail ou senha incorretos");

        rateLimiter.refund(settings.loginPerEmail(), email.getValue());
        rateLimiter.refund(settings.loginPerOrigin(), params.origin());

        var secret = TokenSecret.generate();
        var session = Session.start(
                user.get().getId(),
                secret,
                DeviceLabel.of(params.device()),
                settings.sessionValidity(),
                now
        );

        sessionRepository.save(session);

        return new SessionResponse(secret.getValue(), session.getExpiresAt(), user.get().isMustChangePassword());
    }
}
