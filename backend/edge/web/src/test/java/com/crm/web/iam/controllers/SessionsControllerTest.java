package com.crm.web.iam.controllers;

import com.crm.web.iam.attributes.ActorRequestAttribute;
import com.crm.web.iam.IamMvc;
import com.crm.iam.application.usecases.commands.RevokeSession;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static com.crm.web.iam.IamMvc.ADMINISTRATOR;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("SessionsController")
class SessionsControllerTest {

    private static final UUID SESSION_ID = UUID.fromString("00000000-0000-0000-0000-0000000000d1");

    private final RevokeSession revokeSession = mock(RevokeSession.class);
    private final MockMvc mvc = IamMvc.standalone(new SessionsController(revokeSession));

    @Test
    @DisplayName("encerrar uma sessão responde 204")
    void revoke() throws Exception {
        mvc.perform(delete("/sessions/{id}", SESSION_ID).requestAttr(ActorRequestAttribute.NAME, ADMINISTRATOR))
                .andExpect(status().isNoContent());

        verify(revokeSession).execute(new RevokeSession.Params(ADMINISTRATOR, SESSION_ID));
    }

    @Test
    @DisplayName("encerrar uma sessão sem ator responde 401")
    void revokeWithoutActor() throws Exception {
        mvc.perform(delete("/sessions/{id}", SESSION_ID))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("session_invalid"));

        verifyNoInteractions(revokeSession);
    }
}
