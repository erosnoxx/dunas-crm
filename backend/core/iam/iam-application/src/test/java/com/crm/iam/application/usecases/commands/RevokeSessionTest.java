package com.crm.iam.application.usecases.commands;

import com.crm.common.application.events.EventPublisher;
import com.crm.common.application.exceptions.ForbiddenException;
import com.crm.shared.application.Actor;
import com.crm.iam.application.contracts.repositories.SessionRepository;
import com.crm.iam.domain.entities.Session;
import com.crm.iam.domain.events.SessionRevoked;
import com.crm.shared.domain.enums.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

import static com.crm.iam.application.usecases.commands.IamFixtures.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("RevokeSession")
class RevokeSessionTest {

    private SessionRepository sessionRepository;
    private EventPublisher publisher;
    private RevokeSession revokeSession;

    private final UUID ownerId = UUID.randomUUID();
    private final Session session = session(ownerId);
    private final Actor owner = new Actor(ownerId, Role.TECHNICIAN, UUID.randomUUID());

    @BeforeEach
    void setUp() {
        sessionRepository = mock(SessionRepository.class);
        publisher = mock(EventPublisher.class);

        when(sessionRepository.findById(session.getId())).thenReturn(Optional.of(session));

        revokeSession = new RevokeSession(sessionRepository, publisher, CLOCK);
    }

    @Test
    @DisplayName("sessão que não existe é session_not_found")
    void unknownSession() {
        var unknown = UUID.randomUUID();
        when(sessionRepository.findById(unknown)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> revokeSession.execute(new RevokeSession.Params(owner, unknown)))
                .satisfies(applicationCode("session_not_found"));

        verify(sessionRepository, never()).save(any());
    }

    @Test
    @DisplayName("sessão de outro usuário, por quem não é Administrador, é forbidden e não revoga")
    void anotherUsersSessionIsForbidden() {
        var stranger = new Actor(UUID.randomUUID(), Role.COMMERCIAL, UUID.randomUUID());

        assertThatThrownBy(() -> revokeSession.execute(new RevokeSession.Params(stranger, session.getId())))
                .isInstanceOf(ForbiddenException.class)
                .satisfies(applicationCode("forbidden"));

        assertThat(session.getRevokedAt()).isEmpty();
        verify(sessionRepository, never()).save(any());
        verify(publisher, never()).publishAll(any());
    }

    @Test
    @DisplayName("o dono revoga a própria sessão: salva e depois publica SessionRevoked")
    @SuppressWarnings("unchecked")
    void ownerRevokesOwnSession() {
        revokeSession.execute(new RevokeSession.Params(owner, session.getId()));

        assertThat(session.getRevokedAt()).contains(NOW);
        assertThat(session.getRevokedBy()).contains(ownerId);

        InOrder order = inOrder(sessionRepository, publisher);
        order.verify(sessionRepository).save(session);
        var events = ArgumentCaptor.forClass(Collection.class);
        order.verify(publisher).publishAll(events.capture());

        assertThat((Collection<Object>) events.getValue())
                .singleElement()
                .isInstanceOfSatisfying(SessionRevoked.class, event ->
                        assertThat(event.tokenHash()).isEqualTo(session.getTokenHash()));
    }

    @Test
    @DisplayName("o Administrador revoga a sessão de qualquer um e fica como autor")
    void administratorRevokesAnySession() {
        var admin = new Actor(UUID.randomUUID(), Role.ADMINISTRATOR, UUID.randomUUID());

        revokeSession.execute(new RevokeSession.Params(admin, session.getId()));

        assertThat(session.getRevokedAt()).contains(NOW);
        assertThat(session.getRevokedBy()).contains(admin.userId());
        verify(sessionRepository).save(session);
    }

    @Test
    @DisplayName("é idempotente: a segunda chamada não publica de novo")
    @SuppressWarnings("unchecked")
    void idempotent() {
        revokeSession.execute(new RevokeSession.Params(owner, session.getId()));
        revokeSession.execute(new RevokeSession.Params(owner, session.getId()));

        var events = ArgumentCaptor.forClass(Collection.class);
        verify(publisher, times(2)).publishAll(events.capture());

        assertThat(events.getAllValues().get(0)).hasSize(1);
        assertThat(events.getAllValues().get(1)).isEmpty();
    }
}
