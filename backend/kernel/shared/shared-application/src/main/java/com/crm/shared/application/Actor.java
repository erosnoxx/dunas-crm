package com.crm.shared.application;

import com.crm.common.application.exceptions.ForbiddenException;
import com.crm.shared.domain.enums.Role;

import java.util.Arrays;
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

    /** Exige que o papel do ator esteja entre os permitidos: a checagem é do use case, nunca do controller. */
    public void requireAnyRole(Role... allowed) {
        if (!Arrays.asList(allowed).contains(role))
            throw new ForbiddenException("forbidden", "você não tem permissão para isto");
    }

    /** Só o Administrador gerencia usuários (ADR-5): atalho de {@code requireAnyRole(Role.ADMINISTRATOR)}. */
    public void requireAdministrator() {
        requireAnyRole(Role.ADMINISTRATOR);
    }
}
