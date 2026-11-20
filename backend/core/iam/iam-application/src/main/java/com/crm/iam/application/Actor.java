package com.crm.iam.application;

import com.crm.common.application.exceptions.ForbiddenException;
import com.crm.iam.domain.vos.Role;

import java.util.UUID;

/**
 * Quem está agindo: o usuário, o papel dele e a sessão pela qual chegou.
 *
 * <p>É montado pela cadeia de segurança a partir da sessão autenticada e entregue aos use cases
 * já pronto; nenhum use case confia em papel vindo da requisição.
 *
 * @param userId    usuário autenticado
 * @param role      papel do usuário
 * @param sessionId sessão usada na requisição
 */
public record Actor(
        UUID userId,
        Role role,
        UUID sessionId
) {

    /** Só o Administrador gerencia usuários (ADR-5): a checagem é do use case, nunca do controller. */
    public void requireAdministrator() {
        if (role != Role.ADMINISTRATOR)
            throw new ForbiddenException("forbidden", "você não tem permissão para isto");
    }
}
