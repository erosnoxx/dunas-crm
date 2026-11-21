package com.crm.iam.application.usecases.commands;

import com.crm.common.application.exceptions.ApplicationException;
import com.crm.common.domain.exceptions.DomainException;
import com.crm.common.application.ratelimit.RateLimit;
import com.crm.iam.application.IamSettings;
import com.crm.iam.domain.entities.OneTimeToken;
import com.crm.iam.domain.entities.Session;
import com.crm.iam.domain.entities.User;
import com.crm.iam.domain.vos.DeviceLabel;
import com.crm.iam.domain.vos.PasswordHash;
import com.crm.iam.domain.vos.Role;
import com.crm.iam.domain.vos.TokenPurpose;
import com.crm.iam.domain.vos.TokenSecret;
import com.crm.iam.domain.vos.UserName;
import com.crm.iam.domain.vos.UserStatus;
import com.crm.shared.domain.vos.contact.Email;
import org.assertj.core.api.ThrowingConsumer;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** Dados de teste compartilhados pelos testes dos use cases de autenticação. */
final class IamFixtures {

    static final Instant NOW = Instant.parse("2026-11-19T12:00:00Z");

    static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    static final String LIMITED = "muitas tentativas; tente de novo em alguns minutos";

    static final RateLimit LOGIN_PER_EMAIL = RateLimit.failClosed(
            "iam.login.email", 5, Duration.ofMinutes(15), "rate_limited", LIMITED);

    static final RateLimit LOGIN_PER_ORIGIN = RateLimit.failClosed(
            "iam.login.origin", 20, Duration.ofMinutes(15), "rate_limited", LIMITED);

    static final RateLimit RESET_PER_EMAIL = RateLimit.failClosed(
            "iam.password-reset.email", 3, Duration.ofHours(1), "rate_limited", LIMITED);

    static final IamSettings SETTINGS = new IamSettings(
            Duration.ofDays(30),
            Duration.ofMinutes(5),
            Duration.ofSeconds(60),
            Duration.ofHours(48),
            Duration.ofHours(1),
            LOGIN_PER_EMAIL,
            LOGIN_PER_ORIGIN,
            RESET_PER_EMAIL
    );

    static final PasswordHash USER_HASH = PasswordHash.of("$argon2id$v=19$m=19456,t=2,p=1$dXNlcg$dXNlcmhhc2g");

    static final PasswordHash NEW_HASH = PasswordHash.of("$argon2id$v=19$m=19456,t=2,p=1$bmV3$bmV3aGFzaA");

    static final PasswordHash DUMMY_HASH = PasswordHash.of("$argon2id$v=19$m=19456,t=2,p=1$ZHVtbXk$ZHVtbXloYXNo");

    static final String STRONG_PASSWORD = "Senha#Forte1";

    static final String WEAK_PASSWORD = "fraca";

    static final String ORIGIN = "203.0.113.7";

    private IamFixtures() {
    }

    static User user(UserStatus status) {
        return User.reconstitute(
                UUID.randomUUID(),
                1L,
                UserName.of("João Silva"),
                Email.of("joao@x.com"),
                Role.COMMERCIAL,
                status,
                status == UserStatus.INVITED ? null : USER_HASH,
                false
        );
    }

    static User userMustChangePassword() {
        var user = user(UserStatus.ACTIVE);

        user.markPasswordChangeRequired();

        return user;
    }

    static Session session(UUID userId) {
        return Session.start(userId, TokenSecret.generate(), DeviceLabel.of("Galaxy A54"), Duration.ofDays(30), NOW);
    }

    static OneTimeToken token(
            UUID userId,
            TokenPurpose purpose,
            Instant issuedAt
    ) {
        return OneTimeToken.issue(userId, purpose, TokenSecret.generate(), Duration.ofHours(1), issuedAt);
    }

    static ThrowingConsumer<Throwable> applicationCode(String code) {
        return e -> assertThat(((ApplicationException) e).getCode()).contains(code);
    }

    static ThrowingConsumer<Throwable> domainCode(String code) {
        return e -> assertThat(((DomainException) e).getCode()).contains(code);
    }
}
