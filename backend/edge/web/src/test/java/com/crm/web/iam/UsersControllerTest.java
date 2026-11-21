package com.crm.web.iam;

import com.crm.common.application.pages.Page;
import com.crm.common.application.pages.Pageable;
import com.crm.iam.application.schemas.SessionSummary;
import com.crm.iam.application.schemas.UserDetail;
import com.crm.iam.application.schemas.UserFilter;
import com.crm.iam.application.schemas.UserSummary;
import com.crm.iam.application.usecases.commands.ChangeUserRole;
import com.crm.iam.application.usecases.commands.DeactivateUser;
import com.crm.iam.application.usecases.commands.InviteUser;
import com.crm.iam.application.usecases.commands.ReactivateUser;
import com.crm.iam.application.usecases.commands.ResendInvitation;
import com.crm.iam.application.usecases.commands.RevokeAllSessions;
import com.crm.iam.application.usecases.queries.GetUser;
import com.crm.iam.application.usecases.queries.ListSessions;
import com.crm.iam.application.usecases.queries.ListUsers;
import com.crm.shared.domain.enums.Role;
import com.crm.iam.domain.vos.UserStatus;
import com.crm.shared.application.schemas.response.UUIDResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static com.crm.web.iam.IamMvc.ADMINISTRATOR;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("UsersController")
class UsersControllerTest {

    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-0000000000c1");
    private static final Instant LAST_USED_AT = Instant.parse("2026-11-19T10:00:00Z");

    private final InviteUser inviteUser = mock(InviteUser.class);
    private final ResendInvitation resendInvitation = mock(ResendInvitation.class);
    private final ChangeUserRole changeUserRole = mock(ChangeUserRole.class);
    private final DeactivateUser deactivateUser = mock(DeactivateUser.class);
    private final ReactivateUser reactivateUser = mock(ReactivateUser.class);
    private final RevokeAllSessions revokeAllSessions = mock(RevokeAllSessions.class);
    private final ListUsers listUsers = mock(ListUsers.class);
    private final GetUser getUser = mock(GetUser.class);
    private final ListSessions listSessions = mock(ListSessions.class);

    private final MockMvc mvc = IamMvc.standalone(new UsersController(
            inviteUser,
            resendInvitation,
            changeUserRole,
            deactivateUser,
            reactivateUser,
            revokeAllSessions,
            listUsers,
            getUser,
            listSessions
    ));

    private static MockHttpServletRequestBuilder withActor(MockHttpServletRequestBuilder builder) {
        return builder.requestAttr(ActorRequestAttribute.NAME, ADMINISTRATOR);
    }

    private static MockHttpServletRequestBuilder json(
            MockHttpServletRequestBuilder builder,
            String body
    ) {
        return withActor(builder).contentType(MediaType.APPLICATION_JSON).content(body);
    }

    private static UserSummary summary() {
        return new UserSummary(USER_ID, "Maria", "maria@x.com", Role.ADMINISTRATOR, UserStatus.ACTIVE, LAST_USED_AT);
    }

    @Test
    @DisplayName("convidar responde 201 com o id do usuário")
    void invite() throws Exception {
        when(inviteUser.execute(any())).thenReturn(new UUIDResponse(USER_ID));

        mvc.perform(json(post("/users"), """
                        {"name": "Maria", "email": "maria@x.com", "role": "ADMINISTRATOR"}
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(USER_ID.toString()));

        verify(inviteUser).execute(new InviteUser.Params(ADMINISTRATOR, "Maria", "maria@x.com", "ADMINISTRATOR"));
    }

    @Test
    @DisplayName("convidar com corpo inválido responde 400 com invalidParams")
    void inviteWithInvalidBody() throws Exception {
        mvc.perform(json(post("/users"), """
                        {"name": "", "email": "nao-e-email"}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.invalidParams[?(@.name == 'name')]").exists())
                .andExpect(jsonPath("$.invalidParams[?(@.name == 'email')]").exists())
                .andExpect(jsonPath("$.invalidParams[?(@.name == 'role')]").exists());

        verifyNoInteractions(inviteUser);
    }

    @Test
    @DisplayName("convidar sem ator responde 401")
    void inviteWithoutActor() throws Exception {
        mvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Maria", "email": "maria@x.com", "role": "ADMINISTRATOR"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("session_invalid"));
    }

    @Test
    @DisplayName("lista paginada passa filtro e página ao use case")
    void list() throws Exception {
        var filter = new UserFilter(Role.ADMINISTRATOR, UserStatus.ACTIVE, "mar");

        when(listUsers.execute(any())).thenReturn(new Page<>(List.of(summary()), 1, 10, 11, 2));

        mvc.perform(withActor(get("/users"))
                        .param("page", "1")
                        .param("size", "10")
                        .param("role", "ADMINISTRATOR")
                        .param("status", "ACTIVE")
                        .param("text", "mar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].email").value("maria@x.com"))
                .andExpect(jsonPath("$.totalElements").value(11))
                .andExpect(jsonPath("$.totalPages").value(2));

        verify(listUsers).execute(new ListUsers.Params(ADMINISTRATOR, filter, Pageable.of(1, 10)));
    }

    @Test
    @DisplayName("lista sem parâmetros usa a primeira página e nenhum filtro")
    void listDefaults() throws Exception {
        when(listUsers.execute(any())).thenReturn(new Page<>(List.of(), 0, 20, 0, 0));

        mvc.perform(withActor(get("/users")))
                .andExpect(status().isOk());

        verify(listUsers).execute(new ListUsers.Params(ADMINISTRATOR, new UserFilter(null, null, null), Pageable.of(0, 20)));
    }

    @Test
    @DisplayName("lista com papel desconhecido responde 400")
    void listWithUnknownRole() throws Exception {
        mvc.perform(withActor(get("/users")).param("role", "CHEFE"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(listUsers);
    }

    @Test
    @DisplayName("detalhe devolve o usuário com as sessões")
    void detail() throws Exception {
        var session = new SessionSummary(UUID.fromString("00000000-0000-0000-0000-0000000000d1"), "Firefox", LAST_USED_AT, LAST_USED_AT, false);

        when(getUser.execute(any())).thenReturn(new UserDetail(summary(), List.of(session)));

        mvc.perform(withActor(get("/users/{id}", USER_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.name").value("Maria"))
                .andExpect(jsonPath("$.sessions[0].device").value("Firefox"));

        verify(getUser).execute(new GetUser.Params(ADMINISTRATOR, USER_ID));
    }

    @Test
    @DisplayName("reenviar convite responde 204")
    void resend() throws Exception {
        mvc.perform(withActor(post("/users/{id}/invitation", USER_ID)))
                .andExpect(status().isNoContent());

        verify(resendInvitation).execute(new ResendInvitation.Params(ADMINISTRATOR, USER_ID));
    }

    @Test
    @DisplayName("mudar papel responde 204")
    void changeRole() throws Exception {
        mvc.perform(json(put("/users/{id}/role", USER_ID), """
                        {"role": "ADMINISTRATOR"}
                        """))
                .andExpect(status().isNoContent());

        verify(changeUserRole).execute(new ChangeUserRole.Params(ADMINISTRATOR, USER_ID, "ADMINISTRATOR"));
    }

    @Test
    @DisplayName("mudar papel sem o papel responde 400 com invalidParams")
    void changeRoleWithInvalidBody() throws Exception {
        mvc.perform(json(put("/users/{id}/role", USER_ID), "{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.invalidParams[?(@.name == 'role')]").exists());

        verifyNoInteractions(changeUserRole);
    }

    @Test
    @DisplayName("desativar responde 204")
    void deactivate() throws Exception {
        mvc.perform(withActor(post("/users/{id}/deactivation", USER_ID)))
                .andExpect(status().isNoContent());

        verify(deactivateUser).execute(new DeactivateUser.Params(ADMINISTRATOR, USER_ID));
    }

    @Test
    @DisplayName("reativar responde 204")
    void reactivate() throws Exception {
        mvc.perform(withActor(post("/users/{id}/reactivation", USER_ID)))
                .andExpect(status().isNoContent());

        verify(reactivateUser).execute(new ReactivateUser.Params(ADMINISTRATOR, USER_ID));
    }

    @Test
    @DisplayName("lista as sessões do usuário")
    void sessions() throws Exception {
        var session = new SessionSummary(UUID.fromString("00000000-0000-0000-0000-0000000000d1"), "Firefox", LAST_USED_AT, LAST_USED_AT, true);

        when(listSessions.execute(any())).thenReturn(List.of(session));

        mvc.perform(withActor(get("/users/{id}/sessions", USER_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].current").value(true));

        verify(listSessions).execute(new ListSessions.Params(ADMINISTRATOR, USER_ID));
    }

    @Test
    @DisplayName("encerrar sessões usa keepCurrent falso por padrão")
    void revokeAll() throws Exception {
        mvc.perform(withActor(delete("/users/{id}/sessions", USER_ID)))
                .andExpect(status().isNoContent());

        verify(revokeAllSessions).execute(new RevokeAllSessions.Params(ADMINISTRATOR, USER_ID, false));
    }

    @Test
    @DisplayName("encerrar sessões repassa keepCurrent")
    void revokeAllKeepingCurrent() throws Exception {
        mvc.perform(withActor(delete("/users/{id}/sessions", USER_ID)).param("keepCurrent", "true"))
                .andExpect(status().isNoContent());

        verify(revokeAllSessions).execute(new RevokeAllSessions.Params(ADMINISTRATOR, USER_ID, true));
    }
}
