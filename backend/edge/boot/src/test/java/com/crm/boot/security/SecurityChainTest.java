package com.crm.boot.security;

import com.crm.common.application.exceptions.UnauthorizedException;
import com.crm.shared.application.Actor;
import com.crm.iam.application.schemas.CurrentUser;
import com.crm.iam.application.schemas.SessionResponse;
import com.crm.iam.application.usecases.commands.AcceptInvitation;
import com.crm.iam.application.usecases.commands.AuthenticateSession;
import com.crm.iam.application.usecases.commands.AuthenticateSession.AuthenticatedSession;
import com.crm.iam.application.usecases.commands.ChangePassword;
import com.crm.iam.application.usecases.commands.Login;
import com.crm.iam.application.usecases.commands.Logout;
import com.crm.iam.application.usecases.commands.RequestPasswordReset;
import com.crm.iam.application.usecases.commands.ResetPassword;
import com.crm.iam.application.usecases.queries.GetCurrentUser;
import com.crm.shared.domain.enums.Role;
import com.crm.web.iam.controllers.AuthController;
import com.crm.web.iam.cookies.SessionCookie;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Cadeia de segurança de ponta a ponta no MVC: o filtro de sessão, o CSRF e a sessão restrita, com
 * os controllers de autenticação reais e os use cases mockados. Sem banco, Redis nem broker.
 */
@WebMvcTest(controllers = AuthController.class)
@Import({SecurityConfig.class, SessionCookie.class})
@DisplayName("cadeia de segurança")
class SecurityChainTest {

    private static final String TOKEN = "tok-segredo-da-sessao";
    private static final String RESTRICTED_TOKEN = "tok-restrito";
    private static final String INVALID_TOKEN = "tok-invalido";
    private static final String CSRF = "csrf-do-teste";
    private static final Actor ACTOR = new Actor(
            UUID.fromString("00000000-0000-0000-0000-0000000000a1"),
            Role.ADMINISTRATOR,
            UUID.fromString("00000000-0000-0000-0000-0000000000b1")
    );
    private static final CurrentUser CURRENT_USER = new CurrentUser(ACTOR.userId(), "João", "joao@x.com", Role.ADMINISTRATOR, false);
    private static final SessionResponse OPENED = new SessionResponse(TOKEN, Instant.parse("2026-12-19T10:00:00Z"), false);

    private static final String PASSWORD_BODY = """
            {"currentPassword": "Antiga#Forte1", "newPassword": "Nova#Forte12"}
            """;
    private static final String EMAIL_BODY = """
            {"email": "joao@x.com"}
            """;
    private static final String LINK_BODY = """
            {"token": "link-secreto", "password": "Nova#Forte12"}
            """;
    private static final String LOGIN_BODY = """
            {"email": "joao@x.com", "password": "Senha#Forte1"}
            """;

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private AuthenticateSession authenticateSession;

    @MockitoBean
    private Login login;

    @MockitoBean
    private Logout logout;

    @MockitoBean
    private AcceptInvitation acceptInvitation;

    @MockitoBean
    private RequestPasswordReset requestPasswordReset;

    @MockitoBean
    private ResetPassword resetPassword;

    @MockitoBean
    private ChangePassword changePassword;

    @MockitoBean
    private GetCurrentUser getCurrentUser;

    @BeforeEach
    void setUp() {
        when(authenticateSession.execute(TOKEN)).thenReturn(new AuthenticatedSession(ACTOR, false));
        when(authenticateSession.execute(RESTRICTED_TOKEN)).thenReturn(new AuthenticatedSession(ACTOR, true));
        when(authenticateSession.execute(INVALID_TOKEN))
                .thenThrow(new UnauthorizedException("session_invalid", "sua sessão não vale mais; entre de novo"));
        when(getCurrentUser.execute(any())).thenReturn(CURRENT_USER);
    }

    private static Cookie session(String token) {
        return new Cookie("crm_session", token);
    }

    /** Escrita de navegador: o cookie de CSRF e o cabeçalho com o mesmo valor. */
    private static MockHttpServletRequestBuilder withCsrf(MockHttpServletRequestBuilder builder) {
        return builder.cookie(new Cookie("XSRF-TOKEN", CSRF)).header("X-XSRF-TOKEN", CSRF);
    }

    private static MockHttpServletRequestBuilder json(
            MockHttpServletRequestBuilder builder,
            String body
    ) {
        return builder.contentType(MediaType.APPLICATION_JSON).content(body);
    }

    @Test
    @DisplayName("rota protegida sem cookie dá 401 session_invalid em JSON")
    void protectedRouteWithoutCookieIsUnauthorized() throws Exception {
        mvc.perform(get("/users"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code").value("session_invalid"))
                .andExpect(jsonPath("$.detail").value("sua sessão não vale mais; entre de novo"));

        verifyNoInteractions(authenticateSession);
    }

    @Test
    @DisplayName("token inválido dá 401 session_invalid em JSON e não devolve o token")
    void invalidTokenIsUnauthorized() throws Exception {
        var result = mvc.perform(get("/auth/me").cookie(session(INVALID_TOKEN)))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code").value("session_invalid"))
                .andReturn();

        assertThat(result.getResponse().getContentAsString()).doesNotContain(INVALID_TOKEN);
    }

    @Test
    @DisplayName("token válido chega ao controller com o Actor certo")
    void validTokenReachesTheControllerWithTheActor() throws Exception {
        mvc.perform(get("/auth/me").cookie(session(TOKEN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("joao@x.com"));

        verify(getCurrentUser).execute(ACTOR);
    }

    @Test
    @DisplayName("sessão com mustChangePassword em GET /users dá 403 password_change_required")
    void restrictedSessionCannotCallOtherRoutes() throws Exception {
        mvc.perform(get("/users").cookie(session(RESTRICTED_TOKEN)))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.code").value("password_change_required"))
                .andExpect(jsonPath("$.detail").value("você precisa trocar a senha antes de continuar"));
    }

    @Test
    @DisplayName("a barra final e as rotas vizinhas não escapam da sessão restrita")
    void trailingSlashAndNeighbourRoutesDoNotEscapeTheRestrictedSession() throws Exception {
        mvc.perform(get("/users/").cookie(session(RESTRICTED_TOKEN)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("password_change_required"));

        mvc.perform(withCsrf(post("/auth/sessions")).cookie(session(RESTRICTED_TOKEN)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("password_change_required"));

        mvc.perform(withCsrf(put("/auth/logout")).cookie(session(RESTRICTED_TOKEN)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("password_change_required"));
    }

    @Test
    @DisplayName("sessão restrita ainda troca a senha, sai e consulta quem é")
    void restrictedSessionStillCallsTheThreeAllowedRoutes() throws Exception {
        mvc.perform(withCsrf(json(put("/auth/password"), PASSWORD_BODY)).cookie(session(RESTRICTED_TOKEN)))
                .andExpect(status().isNoContent());

        mvc.perform(withCsrf(post("/auth/logout")).cookie(session(RESTRICTED_TOKEN)))
                .andExpect(status().isNoContent());

        mvc.perform(get("/auth/me").cookie(session(RESTRICTED_TOKEN)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("as quatro rotas públicas respondem sem sessão, e a sessão nem é consultada")
    void publicRoutesNeedNoSession() throws Exception {
        when(login.execute(any())).thenReturn(OPENED);
        when(acceptInvitation.execute(any())).thenReturn(OPENED);

        mvc.perform(withCsrf(json(post("/auth/login"), LOGIN_BODY)))
                .andExpect(status().isOk());

        mvc.perform(withCsrf(json(post("/auth/invitation/accept"), LINK_BODY)))
                .andExpect(status().isOk());

        mvc.perform(withCsrf(json(post("/auth/password/forgot"), EMAIL_BODY)))
                .andExpect(status().isNoContent());

        mvc.perform(withCsrf(json(post("/auth/password/reset"), LINK_BODY)))
                .andExpect(status().isNoContent());

        verifyNoInteractions(authenticateSession);
    }

    @Test
    @DisplayName("cookie velho em rota pública não derruba a rota: a sessão nem é consultada")
    void staleCookieDoesNotBreakAPublicRoute() throws Exception {
        mvc.perform(withCsrf(json(post("/auth/password/forgot"), EMAIL_BODY)).cookie(session(INVALID_TOKEN)))
                .andExpect(status().isNoContent());

        verifyNoInteractions(authenticateSession);
    }

    @Test
    @DisplayName("GET /actuator/health é público")
    void healthIsPublic() throws Exception {
        // o slice não tem o actuator: 404 prova que a cadeia deixou passar (rota protegida daria 401)
        mvc.perform(get("/actuator/health"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("rota pública de escrita sem o cabeçalho de CSRF dá 403 em JSON")
    void publicWriteWithoutCsrfHeaderIsForbidden() throws Exception {
        mvc.perform(json(post("/auth/password/forgot"), EMAIL_BODY).cookie(new Cookie("XSRF-TOKEN", CSRF)))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.code").value("csrf_invalid"));
    }

    @Test
    @DisplayName("cookie de sessão sem X-XSRF-TOKEN em escrita dá 403, e com o cabeçalho certo passa")
    void sessionCookieWriteNeedsTheCsrfHeader() throws Exception {
        mvc.perform(post("/auth/logout").cookie(session(TOKEN)).cookie(new Cookie("XSRF-TOKEN", CSRF)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("csrf_invalid"));

        mvc.perform(post("/auth/logout")
                        .cookie(session(TOKEN))
                        .cookie(new Cookie("XSRF-TOKEN", CSRF))
                        .header("X-XSRF-TOKEN", "outro"))
                .andExpect(status().isForbidden());

        mvc.perform(withCsrf(post("/auth/logout")).cookie(session(TOKEN)))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("um GET sem sessão devolve 401 e o cookie XSRF-TOKEN legível, que vale no POST seguinte")
    void unauthorizedGetEmitsTheCsrfCookieThatTheNextPostAccepts() throws Exception {
        var response = mvc.perform(get("/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("session_invalid"))
                .andReturn()
                .getResponse();

        var setCookie = response.getHeaders(HttpHeaders.SET_COOKIE).stream()
                .filter(value -> value.startsWith("XSRF-TOKEN="))
                .findFirst()
                .orElseThrow();

        assertThat(setCookie).doesNotContain("HttpOnly");

        var csrf = response.getCookie("XSRF-TOKEN").getValue();

        assertThat(csrf).isNotBlank();

        mvc.perform(json(post("/auth/password/forgot"), EMAIL_BODY)
                        .cookie(new Cookie("XSRF-TOKEN", csrf))
                        .header("X-XSRF-TOKEN", csrf))
                .andExpect(status().isNoContent());

        mvc.perform(json(post("/auth/password/forgot"), EMAIL_BODY).cookie(new Cookie("XSRF-TOKEN", csrf)))
                .andExpect(status().isForbidden());
    }
}
