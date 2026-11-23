package com.crm.iam.domain.entities;

import com.crm.common.domain.entities.AggregateRoot;
import com.crm.common.domain.exceptions.DomainException;
import com.crm.iam.domain.events.OneTimeTokenIssued;
import com.crm.iam.domain.vos.TokenHash;
import com.crm.iam.domain.enums.TokenPurpose;
import com.crm.iam.domain.vos.TokenSecret;
import lombok.Getter;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Link de uso único: o convite e a redefinição de senha.
 *
 * <p>O aggregate guarda só o {@link TokenHash}; o {@link TokenSecret} existe apenas no evento
 * {@link OneTimeTokenIssued}, que é domain event e nunca sai do processo. O estado (pendente,
 * usado, invalidado, expirado) é derivado de {@code usedAt}, {@code invalidatedAt} e {@code expiresAt},
 * sem enum. Quem perde a disputa pelo mesmo link recebe {@code link_used}: o update condicional no
 * banco é responsabilidade da infrastructure.
 */
@Getter
public class OneTimeToken extends AggregateRoot {

    private final UUID userId;

    private final TokenPurpose purpose;

    private final TokenHash tokenHash;

    private final Instant expiresAt;

    private Instant usedAt;

    private Instant invalidatedAt;

    private OneTimeToken(
            UUID id,
            Long version,
            UUID userId,
            TokenPurpose purpose,
            TokenHash tokenHash,
            Instant expiresAt,
            Instant usedAt,
            Instant invalidatedAt
    ) {
        super(id, version);

        this.userId = userId;
        this.purpose = purpose;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
        this.usedAt = usedAt;
        this.invalidatedAt = invalidatedAt;
    }

    /** Emite o link: guarda o hash do segredo e registra o evento que leva o segredo ao e-mail. */
    public static OneTimeToken issue(
            UUID userId,
            TokenPurpose purpose,
            TokenSecret secret,
            Duration validity,
            Instant now
    ) {
        var token = new OneTimeToken(
                UUID.randomUUID(),
                null,
                userId,
                purpose,
                secret.hash(),
                now.plus(validity),
                null,
                null
        );

        token.registerEvent(new OneTimeTokenIssued(
                token.getId(),
                now,
                userId,
                purpose,
                secret
        ));

        return token;
    }

    public static OneTimeToken reconstitute(
            UUID id,
            Long version,
            UUID userId,
            TokenPurpose purpose,
            TokenHash tokenHash,
            Instant expiresAt,
            Instant usedAt,
            Instant invalidatedAt
    ) {
        return new OneTimeToken(
                id,
                version,
                userId,
                purpose,
                tokenHash,
                expiresAt,
                usedAt,
                invalidatedAt
        );
    }

    /** Vazio enquanto o link não foi usado. */
    public Optional<Instant> getUsedAt() {
        return Optional.ofNullable(usedAt);
    }

    /** Vazio enquanto nenhum link mais novo o substituiu. */
    public Optional<Instant> getInvalidatedAt() {
        return Optional.ofNullable(invalidatedAt);
    }

    /** Usa o link. Já usado ou substituído é {@code link_used}; vale até antes de {@code expiresAt}. */
    public void consume(Instant now) {
        if (usedAt != null || invalidatedAt != null)
            throw new DomainException("link_used", "este link já foi usado ou foi substituído");

        if (!now.isBefore(expiresAt))
            throw new DomainException("link_expired", "este link expirou");

        this.usedAt = now;
    }

    /** Substitui o link por um novo. Idempotente, e não mexe em quem já foi usado. */
    public void invalidate(Instant now) {
        if (usedAt != null || invalidatedAt != null)
            return;

        this.invalidatedAt = now;
    }
}
