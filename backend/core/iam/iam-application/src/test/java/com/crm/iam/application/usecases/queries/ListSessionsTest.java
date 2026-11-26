package com.crm.iam.application.usecases.queries;

import com.crm.common.application.exceptions.ForbiddenException;
import com.crm.shared.application.Actor;
import com.crm.iam.application.contracts.readers.SessionReader;
import com.crm.iam.application.schemas.SessionSummary;
import com.crm.shared.domain.enums.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@DisplayName("ListSessions")
class ListSessionsTest {

    private static final Instant NOW = Instant.parse("2026-11-19T12:00:00Z");

    private SessionReader sessionReader;
    private ListSessions listSessions;

    @BeforeEach
    void setUp() {
        sessionReader = mock(SessionReader.class);
        listSessions = new ListSessions(sessionReader, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    @DisplayName("o próprio usuário lista as dele, e só a sessão do Actor volta como corrente")
    void ownSessionsMarkTheActorsSessionAsCurrent() {
        var actor = new Actor(UUID.randomUUID(), Role.TECHNICIAN, UUID.randomUUID());
        var current = new SessionSummary(actor.sessionId(), "Galaxy A54", NOW, NOW, true);
        var other = new SessionSummary(UUID.randomUUID(), "Chrome, Windows", NOW, NOW.minusSeconds(60), false);
        when(sessionReader.findActiveByUser(actor.userId(), actor.sessionId(), NOW))
                .thenReturn(List.of(current, other));

        var sessions = listSessions.execute(new ListSessions.Params(actor, actor.userId()));

        assertThat(sessions).containsExactly(current, other);
        assertThat(sessions).filteredOn(SessionSummary::current).containsExactly(current);
    }

    @Test
    @DisplayName("o Administrador lista as sessões de outro usuário")
    void administratorListsAnotherUsersSessions() {
        var administrator = new Actor(UUID.randomUUID(), Role.ADMINISTRATOR, UUID.randomUUID());
        var targetId = UUID.randomUUID();
        var session = new SessionSummary(UUID.randomUUID(), "Galaxy A54", NOW, NOW, false);
        when(sessionReader.findActiveByUser(targetId, administrator.sessionId(), NOW)).thenReturn(List.of(session));

        assertThat(listSessions.execute(new ListSessions.Params(administrator, targetId))).containsExactly(session);
    }

    @Test
    @DisplayName("sessões de outro usuário, por quem não é Administrador, são forbidden e o leitor nem é consultado")
    void anotherUsersSessionsAreForbidden() {
        var actor = new Actor(UUID.randomUUID(), Role.COMMERCIAL, UUID.randomUUID());

        assertThatThrownBy(() -> listSessions.execute(new ListSessions.Params(actor, UUID.randomUUID())))
                .isInstanceOf(ForbiddenException.class);

        verifyNoInteractions(sessionReader);
    }
}
