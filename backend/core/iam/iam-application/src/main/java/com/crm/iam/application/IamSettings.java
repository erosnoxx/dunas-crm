package com.crm.iam.application;

import com.crm.common.application.exceptions.InternalServerErrorException;
import com.crm.common.application.ratelimit.RateLimit;

import java.time.Duration;

/**
 * Valores configuráveis do IAM que os use cases consomem: validades, intervalo de renovação,
 * TTL do cache de sessão e limites de tentativa.
 *
 * <p>É um record de application, sem framework: a infrastructure o monta a partir das
 * propriedades. O construtor recusa nulo e duração não positiva, para que uma configuração errada
 * quebre na subida e não no primeiro login.
 *
 * @param sessionValidity       validade da sessão a partir do último uso
 * @param sessionRenewInterval  intervalo mínimo entre renovações da sessão
 * @param sessionCacheTtl       tempo de vida da sessão no cache
 * @param invitationValidity    validade do link de convite
 * @param passwordResetValidity validade do link de redefinição de senha
 * @param loginPerEmail         limite de erros de login por e-mail
 * @param loginPerOrigin        limite de erros de login por origem
 * @param passwordResetPerEmail limite de pedidos de redefinição por e-mail
 */
public record IamSettings(
        Duration sessionValidity,
        Duration sessionRenewInterval,
        Duration sessionCacheTtl,
        Duration invitationValidity,
        Duration passwordResetValidity,
        RateLimit loginPerEmail,
        RateLimit loginPerOrigin,
        RateLimit passwordResetPerEmail
) {
    public IamSettings {
        requirePositive(sessionValidity, "sessionValidity");
        requirePositive(sessionRenewInterval, "sessionRenewInterval");
        requirePositive(sessionCacheTtl, "sessionCacheTtl");
        requirePositive(invitationValidity, "invitationValidity");
        requirePositive(passwordResetValidity, "passwordResetValidity");

        requirePresent(loginPerEmail, "loginPerEmail");
        requirePresent(loginPerOrigin, "loginPerOrigin");
        requirePresent(passwordResetPerEmail, "passwordResetPerEmail");
    }

    private static void requirePositive(
            Duration value,
            String name
    ) {
        if (value == null || value.isZero() || value.isNegative())
            throw new InternalServerErrorException("configuração do iam sem duração positiva: " + name);
    }

    private static void requirePresent(
            RateLimit value,
            String name
    ) {
        if (value == null)
            throw new InternalServerErrorException("configuração do iam sem limite de tentativas: " + name);
    }
}
