package com.crm.iam.application.usecases.commands;

import com.crm.common.application.events.EventPublisher;
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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("Logout")
class LogoutTest {

    private SessionRepository sessionRepository;
    private EventPublisher publisher;
    private Logout logout;

    private final UUID userId = UUID.randomUUID();
    private final Session session = session(userId);
    private final Actor actor = new Actor(userId, Role.TECHNICIAN, session.getId());

    @BeforeEach
    void setUp() {
        sessionRepository = mock(SessionRepository.class);
        publisher = mock(EventPublisher.class);

        when(sessionRepository.findById(session.getId())).thenReturn(Optional.of(session));

        logout = new Logout(sessionRepository, publisher, CLOCK);
    }

    @Test
    @DisplayName("revoga a sessão do Actor, salva e depois publica SessionRevoked")
    @SuppressWarnings("unchecked")
    void revokesTheActorSession() {
        logout.execute(actor);

        assertThat(session.getRevokedAt()).contains(NOW);
        assertThat(session.getRevokedBy()).contains(userId);

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
    @DisplayName("segunda chamada não publica de novo")
    @SuppressWarnings("unchecked")
    void secondCallDoesNotPublishAgain() {
        logout.execute(actor);
        logout.execute(actor);

        var events = ArgumentCaptor.forClass(Collection.class);
        verify(publisher, times(2)).publishAll(events.capture());

        assertThat(events.getAllValues().get(0)).hasSize(1);
        assertThat(events.getAllValues().get(1)).isEmpty();
    }

    @Test
    @DisplayName("sessão que não existe é session_not_found")
    void unknownSession() {
        when(sessionRepository.findById(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> logout.execute(actor))
                .satisfies(applicationCode("session_not_found"));
    }
}
