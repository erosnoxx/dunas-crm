package com.crm.web.iam;

import com.crm.iam.application.schemas.SessionResponse;

import java.time.Instant;

/**
 * O que o navegador recebe ao abrir uma sessão: nunca o token, que vai só no cookie
 * {@code HttpOnly} (architecture.md §14).
 */
public record SessionView(
        Instant expiresAt,
        boolean mustChangePassword
) {
    static SessionView of(SessionResponse response) {
        return new SessionView(response.expiresAt(), response.mustChangePassword());
    }
}
