package com.crm.web.iam;

import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Monta o cookie de sessão do navegador (architecture.md §14): {@code HttpOnly},
 * {@code SameSite=Lax} e {@code Secure} em todo perfil menos o {@code dev}, onde o
 * desenvolvimento roda em http.
 *
 * <p>O {@code Max-Age} é o teto dos navegadores (400 dias); quem decide se a sessão vale e quando
 * ela vence é o servidor.
 */
@Component
public class SessionCookie {

    static final String NAME = "crm_session";
    private static final Duration MAX_AGE = Duration.ofDays(400);

    private final boolean secure;

    public SessionCookie(Environment environment) {
        this.secure = !environment.acceptsProfiles(Profiles.of("dev"));
    }

    /** Cookie que carrega o token da sessão recém-aberta. */
    public String issue(String token) {
        return build(token, MAX_AGE);
    }

    /** Cookie vazio e vencido, para o navegador esquecer a sessão. */
    public String clear() {
        return build("", Duration.ZERO);
    }

    private String build(
            String value,
            Duration maxAge
    ) {
        return ResponseCookie.from(NAME, value)
                .httpOnly(true)
                .secure(secure)
                .sameSite("Lax")
                .path("/")
                .maxAge(maxAge)
                .build()
                .toString();
    }
}
