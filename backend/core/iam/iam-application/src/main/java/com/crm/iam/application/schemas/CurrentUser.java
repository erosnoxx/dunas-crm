package com.crm.iam.application.schemas;

import com.crm.shared.domain.enums.Role;

import java.util.UUID;

/**
 * Quem está logado, para o cliente montar a interface.
 *
 * @param mustChangePassword se a conta só pode trocar a senha ou sair
 */
public record CurrentUser(
        UUID id,
        String name,
        String email,
        Role role,
        boolean mustChangePassword
) {
}
