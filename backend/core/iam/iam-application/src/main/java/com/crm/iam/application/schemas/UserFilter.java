package com.crm.iam.application.schemas;

import com.crm.shared.domain.enums.Role;
import com.crm.iam.domain.vos.UserStatus;

/**
 * Filtros da lista de usuários; nulo ou vazio em um campo significa sem filtro.
 *
 * @param text trecho do nome ou do e-mail
 */
public record UserFilter(
        Role role,
        UserStatus status,
        String text
) {
}
