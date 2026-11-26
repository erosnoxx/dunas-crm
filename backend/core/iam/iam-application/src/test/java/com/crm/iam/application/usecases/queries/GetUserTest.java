package com.crm.iam.application.usecases.queries;

import com.crm.common.application.exceptions.ApplicationException;
import com.crm.common.application.exceptions.ForbiddenException;
import com.crm.shared.application.Actor;
import com.crm.iam.application.contracts.readers.SessionReader;
import com.crm.iam.application.contracts.readers.UserReader;
import com.crm.iam.application.schemas.SessionSummary;
import com.crm.iam.application.schemas.UserDetail;
import com.crm.iam.application.schemas.UserSummary;
import com.crm.shared.domain.enums.Role;
import com.crm.iam.domain.enums.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@DisplayName("GetUser")
class GetUserTest {

    private static final Instant NOW = Instant.parse("2026-11-19T12:00:00Z");

    private UserReader userReader;
    private SessionReader sessionReader;
    private GetUser getUser;

    private final Actor administrator = new Actor(UUID.randomUUID(), Role.ADMINISTRATOR, UUID.randomUUID());
    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        userReader = mock(UserReader.class);
        sessionReader = mock(SessionReader.class);
        getUser = new GetUser(userReader, sessionReader, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    @DisplayName("devolve o resumo e as sessões ativas do usuário")
    void returnsSummaryAndActiveSessions() {
        var summary = new UserSummary(userId, "Ana Souza", "ana@empresa.com", Role.COMMERCIAL, UserStatus.ACTIVE, NOW);
        var sessions = List.of(new SessionSummary(UUID.randomUUID(), "Galaxy A54", NOW, NOW, false));
        when(userReader.findSummary(userId)).thenReturn(Optional.of(summary));
        when(sessionReader.findActiveByUser(userId, administrator.sessionId(), NOW)).thenReturn(sessions);

        var detail = getUser.execute(new GetUser.Params(administrator, userId));

        assertThat(detail).isEqualTo(new UserDetail(summary, sessions));
    }

    @Test
    @DisplayName("usuário que não existe é user_not_found")
    void unknownUser() {
        when(userReader.findSummary(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> getUser.execute(new GetUser.Params(administrator, userId)))
                .isInstanceOfSatisfying(ApplicationException.class,
                        e -> assertThat(e.getCode()).contains("user_not_found"));
    }

    @Test
    @DisplayName("quem não é Administrador recebe forbidden e nenhum leitor é consultado")
    void nonAdministratorIsForbidden() {
        var commercial = new Actor(UUID.randomUUID(), Role.COMMERCIAL, UUID.randomUUID());

        assertThatThrownBy(() -> getUser.execute(new GetUser.Params(commercial, userId)))
                .isInstanceOf(ForbiddenException.class);

        verifyNoInteractions(userReader, sessionReader);
    }
}
