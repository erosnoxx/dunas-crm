package com.crm;

import java.net.http.HttpClient;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * As contas dos testes ponta a ponta que rodam fora do IAM: o Administrador do seed, já com a senha
 * trocada, e o Técnico convidado pelo fluxo do IAM, cada um no seu {@link Browser}.
 *
 * <p>As constantes vão para as {@code properties} do {@code @SpringBootTest} de quem usa, então o
 * seed do Administrador e o login daqui sempre batem.
 */
final class EndToEndAccounts {

    static final String ADMIN_EMAIL = "admin@dunas.test";
    static final String ADMIN_INITIAL_PASSWORD = "Inicial#2026";
    static final String ADMIN_PASSWORD = "Trocada#2026";
    static final String TECHNICIAN_PASSWORD = "Tecnico#2026";

    private final HttpClient http;
    private final int port;
    private final Set<String> secrets;
    private final CapturingEmailSender emails;

    EndToEndAccounts(
            HttpClient http,
            int port,
            Set<String> secrets,
            CapturingEmailSender emails
    ) {
        this.http = http;
        this.port = port;
        this.secrets = secrets;
        this.emails = emails;
    }

    Browser browser(String device) {
        return new Browser(http, port, secrets, device);
    }

    /** O Administrador logado e com a senha já trocada, seja qual for a ordem dos cenários. */
    Browser admin() {
        var admin = browser("admin-pc");

        admin.get("/auth/me");

        if (admin.post("/auth/login", Map.of("email", ADMIN_EMAIL, "password", ADMIN_PASSWORD)).status() == 200)
            return admin;

        admin.post("/auth/login", Map.of("email", ADMIN_EMAIL, "password", ADMIN_INITIAL_PASSWORD));
        admin.put("/auth/password", Map.of("currentPassword", ADMIN_INITIAL_PASSWORD, "newPassword", ADMIN_PASSWORD));

        return admin;
    }

    /** Convida um Técnico pelo fluxo do IAM e devolve o navegador dele, já com a sessão do aceite. */
    Browser technician(
            Browser admin,
            String email
    ) {
        var invite = admin.post("/users", Map.of("name", "Tecnico Convidado", "email", email, "role", "TECHNICIAN"));

        assertThat(invite.status()).isEqualTo(201);

        var technician = browser("tech-phone");

        technician.get("/auth/me");

        var accepted = technician.post(
                "/auth/invitation/accept",
                Map.of("token", emails.linkTokenSentTo(email), "password", TECHNICIAN_PASSWORD)
        );

        assertThat(accepted.status()).isEqualTo(200);

        return technician;
    }
}
