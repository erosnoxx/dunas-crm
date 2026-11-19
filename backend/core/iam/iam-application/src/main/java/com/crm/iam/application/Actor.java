package com.crm.iam.application;

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
}
