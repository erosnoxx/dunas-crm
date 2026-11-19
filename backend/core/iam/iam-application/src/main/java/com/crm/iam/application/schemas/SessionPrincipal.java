package com.crm.iam.application.schemas;

import com.crm.iam.domain.vos.Role;
import com.crm.iam.domain.vos.UserStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * O que a autenticação precisa saber de uma sessão e do dono dela.
 *
 * <p>Vai para o {@code Cache} serializado em JSON, por isso só tem tipos simples (UUID, enum,
 * boolean, Instant) e nenhum Value Object de domínio.
 *
 * @param revokedAt quando a sessão foi revogada; nulo se está valendo
 */
public record SessionPrincipal(
        UUID sessionId,
        UUID userId,
        Role role,
        UserStatus userStatus,
        boolean mustChangePassword,
        Instant lastUsedAt,
        Instant expiresAt,
        Instant revokedAt
) {
}
