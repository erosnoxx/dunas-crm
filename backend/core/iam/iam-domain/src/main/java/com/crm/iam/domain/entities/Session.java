package com.crm.iam.domain.entities;

import com.crm.common.domain.entities.AggregateRoot;
import com.crm.common.domain.exceptions.DomainException;
import com.crm.iam.domain.events.SessionRevoked;
import com.crm.iam.domain.vos.DeviceLabel;
import com.crm.iam.domain.vos.TokenHash;
import com.crm.iam.domain.vos.TokenSecret;
import lombok.Getter;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Um aparelho conectado à conta (ADR-1).
 *
 * <p>Guarda só o {@link TokenHash}: o {@link TokenSecret} vai ao cliente e nunca é armazenado. Está
 * ativa quando não foi revogada e {@code now < expiresAt}; revogada nunca volta a valer. A duração
 * e o intervalo mínimo entre renovações entram por parâmetro, e a hora também.
 */
@Getter
public class Session extends AggregateRoot {

    private final UUID userId;

    private final TokenHash tokenHash;

    private final DeviceLabel device;

    private final Instant startedAt;

    private Instant lastUsedAt;

    private Instant expiresAt;

    private Instant revokedAt;

    private UUID revokedBy;

    private Session(
            UUID id,
            Long version,
            UUID userId,
            TokenHash tokenHash,
            DeviceLabel device,
            Instant startedAt,
            Instant lastUsedAt,
            Instant expiresAt,
            Instant revokedAt,
            UUID revokedBy
    ) {
        super(id, version);

        this.userId = userId;
        this.tokenHash = tokenHash;
        this.device = device;
        this.startedAt = startedAt;
        this.lastUsedAt = lastUsedAt;
        this.expiresAt = expiresAt;
        this.revokedAt = revokedAt;
        this.revokedBy = revokedBy;
    }

    /** Abre a sessão: guarda o hash do segredo e conta a validade a partir de {@code now}. */
    public static Session start(
            UUID userId,
            TokenSecret secret,
            DeviceLabel device,
            Duration validity,
            Instant now
    ) {
        return new Session(
                UUID.randomUUID(),
                null,
                userId,
                secret.hash(),
                device,
                now,
                now,
                now.plus(validity),
                null,
                null
        );
    }

    public static Session reconstitute(
            UUID id,
            Long version,
            UUID userId,
            TokenHash tokenHash,
            DeviceLabel device,
            Instant startedAt,
            Instant lastUsedAt,
            Instant expiresAt,
            Instant revokedAt,
            UUID revokedBy
    ) {
        return new Session(
                id,
                version,
                userId,
                tokenHash,
                device,
                startedAt,
                lastUsedAt,
                expiresAt,
                revokedAt,
                revokedBy
        );
    }

    /** Vazio enquanto a sessão não foi encerrada. */
    public Optional<Instant> getRevokedAt() {
        return Optional.ofNullable(revokedAt);
    }

    /** Vazio enquanto a sessão não foi encerrada. */
    public Optional<UUID> getRevokedBy() {
        return Optional.ofNullable(revokedBy);
    }

    /**
     * Renova a sessão se já passou {@code minInterval} desde o último uso (ADR-6): devolve
     * {@code true} e move {@code lastUsedAt} e {@code expiresAt}; antes disso devolve {@code false}
     * e não muda nada. Sessão inativa é {@code session_invalid}.
     */
    public boolean renewIfDue(Instant now, Duration validity, Duration minInterval) {
        if (!isActiveAt(now))
            throw new DomainException("session_invalid", "sua sessão não vale mais");

        if (now.isBefore(lastUsedAt.plus(minInterval)))
            return false;

        this.lastUsedAt = now;
        this.expiresAt = now.plus(validity);

        return true;
    }

    /** Encerra a sessão e registra {@link SessionRevoked}. Idempotente: já revogada não muda nada. */
    public void revoke(UUID by, Instant now) {
        if (revokedAt != null)
            return;

        this.revokedAt = now;
        this.revokedBy = by;

        registerEvent(new SessionRevoked(getId(), now, tokenHash));
    }

    /** Ativa quando não foi revogada e {@code now} é anterior a {@code expiresAt}. */
    public boolean isActiveAt(Instant now) {
        return revokedAt == null && now.isBefore(expiresAt);
    }
}
