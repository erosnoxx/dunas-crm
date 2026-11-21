package com.crm.iam.application.usecases.commands;

import com.crm.common.application.events.EventPublisher;
import com.crm.common.application.exceptions.BadRequestException;
import com.crm.common.application.exceptions.ForbiddenException;
import com.crm.iam.application.Actor;
import com.crm.iam.application.contracts.repositories.SessionRepository;
import com.crm.iam.domain.entities.Session;
import com.crm.iam.domain.vos.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static com.crm.iam.application.usecases.commands.IamFixtures.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@DisplayName("RevokeAllSessions")
class RevokeAllSessionsTest {

    private SessionRepository sessionRepository;
    private EventPublisher publisher;
    private RevokeAllSessions revokeAllSessions;

    private final UUID userId = UUID.randomUUID();
    private final Session current = session(userId);
    private final Session other = session(userId);
    private final Actor self = new Actor(userId, Role.TECHNICIAN, current.getId());

    @BeforeEach
    void setUp() {
        sessionRepository = mock(SessionRepository.class);
        publisher = mock(EventPublisher.class);

        when(sessionRepository.findActiveByUserId(userId, NOW)).thenReturn(List.of(current, other));

        revokeAllSessions = new RevokeAllSessions(sessionRepository, publisher, CLOCK);
    }

    @Test
    @DisplayName("usuário diferente, por quem não é Administrador, é forbidden e nenhum repository é tocado")
    void anotherUserIsForbidden() {
        var stranger = new Actor(UUID.randomUUID(), Role.COMMERCIAL, UUID.randomUUID());

        assertThatThrownBy(() -> revokeAllSessions.execute(new RevokeAllSessions.Params(stranger, userId, false)))
                .isInstanceOf(ForbiddenException.class)
                .satisfies(applicationCode("forbidden"));

        verifyNoInteractions(sessionRepository, publisher);
    }

    @Test
    @DisplayName("keepCurrent com o userId de outro é BadRequestException, até para o Administrador")
    void keepCurrentOnlyForOwnUser() {
        var admin = new Actor(UUID.randomUUID(), Role.ADMINISTRATOR, UUID.randomUUID());

        assertThatThrownBy(() -> revokeAllSessions.execute(new RevokeAllSessions.Params(admin, userId, true)))
                .isInstanceOf(BadRequestException.class);

        verifyNoInteractions(sessionRepository, publisher);
    }

    @Test
    @DisplayName("keepCurrent=true preserva a sessão do Actor e revoga as outras")
    void keepsTheCurrentSession() {
        revokeAllSessions.execute(new RevokeAllSessions.Params(self, userId, true));

        assertThat(current.getRevokedAt()).isEmpty();
        assertThat(other.getRevokedAt()).contains(NOW);
        assertThat(other.getRevokedBy()).contains(userId);

        verify(sessionRepository).save(other);
        verify(sessionRepository, never()).save(current);
        verify(publisher).publishAll(any());
    }

    @Test
    @DisplayName("keepCurrent=false revoga todas, inclusive a do Actor")
    void revokesEverything() {
        revokeAllSessions.execute(new RevokeAllSessions.Params(self, userId, false));

        assertThat(current.getRevokedAt()).contains(NOW);
        assertThat(other.getRevokedAt()).contains(NOW);

        verify(sessionRepository).save(current);
        verify(sessionRepository).save(other);
    }

    @Test
    @DisplayName("o Administrador revoga todas as sessões de outro usuário e fica como autor")
    void administratorRevokesAnotherUsersSessions() {
        var admin = new Actor(UUID.randomUUID(), Role.ADMINISTRATOR, UUID.randomUUID());

        revokeAllSessions.execute(new RevokeAllSessions.Params(admin, userId, false));

        assertThat(current.getRevokedBy()).contains(admin.userId());
        assertThat(other.getRevokedBy()).contains(admin.userId());
    }
}
