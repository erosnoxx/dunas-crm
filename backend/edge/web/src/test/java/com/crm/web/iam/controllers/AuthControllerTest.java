package com.crm.web.iam.controllers;

import com.crm.web.iam.cookies.SessionCookie;
import com.crm.web.iam.attributes.ActorRequestAttribute;
import com.crm.web.iam.IamMvc;
import com.crm.common.application.exceptions.UnauthorizedException;
import com.crm.iam.application.schemas.CurrentUser;
import com.crm.iam.application.schemas.SessionResponse;
import com.crm.iam.application.usecases.commands.AcceptInvitation;
import com.crm.iam.application.usecases.commands.ChangePassword;
import com.crm.iam.application.usecases.commands.Login;
import com.crm.iam.application.usecases.commands.Logout;
import com.crm.iam.application.usecases.commands.RequestPasswordReset;
import com.crm.iam.application.usecases.commands.ResetPassword;
import com.crm.iam.application.usecases.queries.GetCurrentUser;
import com.crm.shared.domain.enums.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.Instant;
import java.util.UUID;

import static com.crm.web.iam.IamMvc.ADMINISTRATOR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("AuthController")
class AuthControllerTest {

    private static final Instant EXPIRES_AT = Instant.parse("2026-12-19T10:00:00Z");
    private static final String TOKEN = "tok-segredo-da-sessao";
    private static final String LOGIN_BODY = """
            {"email": "joao@x.com", "password": "Senha#Forte1"}
            """;

    private final Login login = mock(Login.class);
    private final Logout logout = mock(Logout.class);
    private final AcceptInvitation acceptInvitation = mock(AcceptInvitation.class);
    private final RequestPasswordReset requestPasswordReset = mock(RequestPasswordReset.class);
    private final ResetPassword resetPassword = mock(ResetPassword.class);
    private final ChangePassword changePassword = mock(ChangePassword.class);
    private final GetCurrentUser getCurrentUser = mock(GetCurrentUser.class);

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = mvcWithProfile("prod");
    }

    private MockMvc mvcWithProfile(String profile) {
        var environment = new MockEnvironment();

        environment.setActiveProfiles(profile);

        return IamMvc.standalone(new AuthController(
                login,
                logout,
                acceptInvitation,
                requestPasswordReset,
                resetPassword,
                changePassword,
                getCurrentUser,
                new SessionCookie(environment)
        ));
    }

    private static MockHttpServletRequestBuilder json(
            MockHttpServletRequestBuilder builder,
            String body
    ) {
        return builder.contentType(MediaType.APPLICATION_JSON).content(body);
    }

    private static MockHttpServletRequestBuilder withActor(MockHttpServletRequestBuilder builder) {
        return builder.requestAttr(ActorRequestAttribute.NAME, ADMINISTRATOR);
    }

    @Test
    @DisplayName("login abre a sessão e entrega o token só no cookie")
    void loginSetsTheCookieAndHidesTheToken() throws Exception {
        when(login.execute(any())).thenReturn(new SessionResponse(TOKEN, EXPIRES_AT, true));

        var result = mvc.perform(json(post("/auth/login"), LOGIN_BODY).header(HttpHeaders.USER_AGENT, "Firefox"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.expiresAt").exists())
                .andExpect(jsonPath("$.mustChangePassword").value(true))
                .andExpect(jsonPath("$.token").doesNotExist())
                .andReturn();

        assertThat(result.getResponse().getContentAsString()).doesNotContain(TOKEN);

        var cookie = result.getResponse().getHeader(HttpHeaders.SET_COOKIE);

        assertThat(cookie).startsWith("crm_session=" + TOKEN)
                .contains("HttpOnly", "SameSite=Lax", "Path=/", "Max-Age=34560000", "Secure");

        verify(login).execute(new Login.Params("joao@x.com", "Senha#Forte1", "Firefox", "127.0.0.1"));
    }

    @Test
    @DisplayName("no perfil dev o cookie não leva Secure")
    void devProfileDropsSecure() throws Exception {
        when(login.execute(any())).thenReturn(new SessionResponse(TOKEN, EXPIRES_AT, false));

        var cookie = mvcWithProfile("dev").perform(json(post("/auth/login"), LOGIN_BODY))
                .andReturn().getResponse().getHeader(HttpHeaders.SET_COOKIE);

        assertThat(cookie).contains("HttpOnly", "SameSite=Lax").doesNotContain("Secure");
    }

    @Test
    @DisplayName("o rótulo do aparelho prefere X-Device-Name, é truncado em 80 e tem padrão")
    void deviceLabel() throws Exception {
        when(login.execute(any())).thenReturn(new SessionResponse(TOKEN, EXPIRES_AT, false));

        mvc.perform(json(post("/auth/login"), LOGIN_BODY)
                .header("X-Device-Name", "Pixel 8")
                .header(HttpHeaders.USER_AGENT, "okhttp"));
        mvc.perform(json(post("/auth/login"), LOGIN_BODY).header("X-Device-Name", "x".repeat(100)));
        mvc.perform(json(post("/auth/login"), LOGIN_BODY).header("X-Device-Name", "  "));

        verify(login).execute(new Login.Params("joao@x.com", "Senha#Forte1", "Pixel 8", "127.0.0.1"));
        verify(login).execute(new Login.Params("joao@x.com", "Senha#Forte1", "x".repeat(80), "127.0.0.1"));
        verify(login).execute(new Login.Params("joao@x.com", "Senha#Forte1", "Aparelho desconhecido", "127.0.0.1"));
    }

    @Test
    @DisplayName("login com corpo inválido responde 400 com invalidParams e não chama o use case")
    void loginWithInvalidBody() throws Exception {
        mvc.perform(json(post("/auth/login"), """
                        {"email": " "}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.invalidParams[?(@.name == 'email')]").exists())
                .andExpect(jsonPath("$.invalidParams[?(@.name == 'password')]").exists());

        verifyNoInteractions(login);
    }

    @Test
    @DisplayName("erro do use case sai como ProblemDetail com code")
    void useCaseErrorKeepsTheCode() throws Exception {
        when(login.execute(any())).thenThrow(new UnauthorizedException("invalid_credentials", "e-mail ou senha incorretos"));

        mvc.perform(json(post("/auth/login"), LOGIN_BODY))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("invalid_credentials"));
    }

    @Test
    @DisplayName("aceite de convite abre a sessão com cookie e sem token no corpo")
    void acceptInvitation() throws Exception {
        when(acceptInvitation.execute(any())).thenReturn(new SessionResponse(TOKEN, EXPIRES_AT, false));

        var result = mvc.perform(json(post("/auth/invitation/accept"), """
                        {"token": "link-secreto", "password": "Senha#Forte1"}
                        """).header(HttpHeaders.USER_AGENT, "Firefox"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mustChangePassword").value(false))
                .andExpect(jsonPath("$.token").doesNotExist())
                .andReturn();

        assertThat(result.getResponse().getContentAsString()).doesNotContain(TOKEN);
        assertThat(result.getResponse().getHeader(HttpHeaders.SET_COOKIE))
                .startsWith("crm_session=" + TOKEN)
                .contains("HttpOnly", "SameSite=Lax");

        verify(acceptInvitation).execute(new AcceptInvitation.Params("link-secreto", "Senha#Forte1", "Firefox"));
    }

    @Test
    @DisplayName("logout encerra a sessão do ator e limpa o cookie")
    void logoutClearsTheCookie() throws Exception {
        var result = mvc.perform(withActor(post("/auth/logout")))
                .andExpect(status().isNoContent())
                .andReturn();

        assertThat(result.getResponse().getHeader(HttpHeaders.SET_COOKIE))
                .startsWith("crm_session=;")
                .contains("Max-Age=0", "HttpOnly", "SameSite=Lax", "Path=/");

        verify(logout).execute(ADMINISTRATOR);
    }

    @Test
    @DisplayName("logout sem ator responde 401 com session_invalid")
    void logoutWithoutActor() throws Exception {
        mvc.perform(post("/auth/logout"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("session_invalid"));

        verify(logout, never()).execute(any());
    }

    @Test
    @DisplayName("esqueci a senha responde sempre 204 e passa e-mail e origem")
    void forgotPassword() throws Exception {
        mvc.perform(json(post("/auth/password/forgot"), """
                        {"email": "joao@x.com"}
                        """))
                .andExpect(status().isNoContent());

        verify(requestPasswordReset).execute(new RequestPasswordReset.Params("joao@x.com", "127.0.0.1"));
    }

    @Test
    @DisplayName("redefinição de senha responde 204 e passa token e senha")
    void resetPassword() throws Exception {
        mvc.perform(json(post("/auth/password/reset"), """
                        {"token": "link-secreto", "password": "Senha#Forte1"}
                        """))
                .andExpect(status().isNoContent());

        verify(resetPassword).execute(new ResetPassword.Params("link-secreto", "Senha#Forte1"));
    }

    @Test
    @DisplayName("troca de senha usa o ator, não apara a senha e responde 204")
    void changePassword() throws Exception {
        mvc.perform(json(withActor(put("/auth/password")), """
                        {"currentPassword": " Atual#Senha1", "newPassword": "Nova#Senha22 "}
                        """))
                .andExpect(status().isNoContent());

        verify(changePassword).execute(new ChangePassword.Params(ADMINISTRATOR, " Atual#Senha1", "Nova#Senha22 "));
    }

    @Test
    @DisplayName("troca de senha com corpo inválido responde 400 com invalidParams")
    void changePasswordWithInvalidBody() throws Exception {
        mvc.perform(json(withActor(put("/auth/password")), """
                        {"currentPassword": "Atual#Senha1"}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.invalidParams[?(@.name == 'newPassword')]").exists());

        verifyNoInteractions(changePassword);
    }

    @Test
    @DisplayName("troca de senha sem ator responde 401")
    void changePasswordWithoutActor() throws Exception {
        mvc.perform(json(put("/auth/password"), """
                        {"currentPassword": "Atual#Senha1", "newPassword": "Nova#Senha22"}
                        """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("me devolve o usuário do ator")
    void me() throws Exception {
        var id = UUID.fromString("00000000-0000-0000-0000-0000000000a1");

        when(getCurrentUser.execute(ADMINISTRATOR))
                .thenReturn(new CurrentUser(id, "João", "joao@x.com", Role.ADMINISTRATOR, false));

        mvc.perform(withActor(get("/auth/me")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("joao@x.com"))
                .andExpect(jsonPath("$.role").value("ADMINISTRATOR"))
                .andExpect(jsonPath("$.mustChangePassword").value(false));
    }

    @Test
    @DisplayName("me sem ator responde 401")
    void meWithoutActor() throws Exception {
        mvc.perform(get("/auth/me"))
                .andExpect(status().isUnauthorized());
    }
}
