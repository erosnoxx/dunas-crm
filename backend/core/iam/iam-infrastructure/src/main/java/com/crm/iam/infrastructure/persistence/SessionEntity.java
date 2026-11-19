package com.crm.iam.infrastructure.persistence;

import com.crm.common.infrastructure.persistence.entities.JpaEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Linha da tabela {@code sessions}: guarda só o hash do segredo da sessão. Sem {@code toString}
 * gerado.
 */
@Entity
@Table(name = "sessions")
@Getter
@Setter
@NoArgsConstructor
public class SessionEntity extends JpaEntity<UUID> {

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private String tokenHash;

    @Column(nullable = false)
    private String device;

    @Column(nullable = false)
    private Instant startedAt;

    @Column(nullable = false)
    private Instant lastUsedAt;

    @Column(nullable = false)
    private Instant expiresAt;

    private Instant revokedAt;

    private UUID revokedBy;
}
