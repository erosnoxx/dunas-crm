package com.crm.iam.infrastructure.persistence.entities;

import com.crm.common.infrastructure.persistence.entities.JpaEntity;
import com.crm.iam.domain.enums.TokenPurpose;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Linha da tabela {@code one_time_tokens}: guarda só o hash do segredo do link. Sem
 * {@code toString} gerado.
 */
@Entity
@Table(name = "one_time_tokens")
@Getter
@Setter
@NoArgsConstructor
public class OneTimeTokenEntity extends JpaEntity<UUID> {

    @Column(nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TokenPurpose purpose;

    @Column(nullable = false)
    private String tokenHash;

    @Column(nullable = false)
    private Instant expiresAt;

    private Instant usedAt;

    private Instant invalidatedAt;
}
