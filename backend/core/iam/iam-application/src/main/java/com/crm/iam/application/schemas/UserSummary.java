package com.crm.iam.application.schemas;

import com.crm.shared.domain.enums.Role;
import com.crm.iam.domain.enums.UserStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * Linha da lista de usuários.
 *
 * @param lastUsedAt último uso entre as sessões do usuário; nulo se nunca entrou
 */
public record UserSummary(
        UUID id,
        String name,
        String email,
        Role role,
        UserStatus status,
        Instant lastUsedAt
) {
}
