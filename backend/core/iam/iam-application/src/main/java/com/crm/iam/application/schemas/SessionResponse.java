package com.crm.iam.application.schemas;

import java.time.Instant;

/**
 * Sessão aberta por login ou aceite de convite.
 *
 * @param token              segredo da sessão, devolvido uma única vez e nunca guardado em claro
 * @param expiresAt          quando a sessão expira se não for usada
 * @param mustChangePassword se o usuário precisa trocar a senha antes de qualquer outra coisa
 */
public record SessionResponse(
        String token,
        Instant expiresAt,
        boolean mustChangePassword
) {
    /** Sem o segredo da sessão: o log e o {@code toString()} não podem vazá-lo. */
    @Override
    public String toString() {
        return "SessionResponse[expiresAt=" + expiresAt + ", mustChangePassword=" + mustChangePassword + "]";
    }
}
