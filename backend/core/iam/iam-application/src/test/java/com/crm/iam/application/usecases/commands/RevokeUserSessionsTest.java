package com.crm.iam.application.usecases.commands;

import com.crm.common.application.events.EventPublisher;
import com.crm.iam.application.contracts.repositories.SessionRepository;
import com.crm.iam.domain.entities.Session;
import com.crm.iam.domain.events.SessionRevoked;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import static com.crm.iam.application.usecases.commands.IamFixtures.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@DisplayName("RevokeUserSessions")
class RevokeUserSessionsTest {

    private SessionRepository sessionRepository;
    private EventPublisher publisher;
    private RevokeUserSessions revokeUserSessions;

    private final UUID userId = UUID.randomUUID();
    private final UUID deactivatedBy = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        sessionRepository = mock(SessionRepository.class);
        publisher = mock(EventPublisher.class);

        revokeUserSessions = new RevokeUserSessions(sessionRepository, publisher, CLOCK);
    }

    @Test
    @DisplayName("revoga todas as sessões ativas, com o autor da desativação, e publica um SessionRevoked por sessão")
    @SuppressWarnings("unchecked")
    void revokesAllActiveSessions() {
        var first = session(userId);
        var second = session(userId);
        when(sessionRepository.findActiveByUserId(userId, NOW)).thenReturn(List.of(first, second));

        revokeUserSessions.execute(new RevokeUserSessions.Params(userId, deactivatedBy));

        assertThat(first.getRevokedAt()).contains(NOW);
        assertThat(first.getRevokedBy()).contains(deactivatedBy);
        assertThat(second.getRevokedAt()).contains(NOW);

        verify(sessionRepository).save(first);
        verify(sessionRepository).save(second);

        var events = ArgumentCaptor.forClass(Collection.class);
        verify(publisher, times(2)).publishAll(events.capture());

        assertThat(events.getAllValues().stream().flatMap(Collection::stream))
                .hasSize(2)
                .allSatisfy(event -> assertThat(event).isInstanceOf(SessionRevoked.class));
    }

    @Test
    @DisplayName("sem sessões ativas, não salva nem publica nada")
    void noSessions() {
        when(sessionRepository.findActiveByUserId(userId, NOW)).thenReturn(List.of());

        revokeUserSessions.execute(new RevokeUserSessions.Params(userId, deactivatedBy));

        verifyNoInteractions(publisher);
        verify(sessionRepository).findActiveByUserId(userId, NOW);
        verifyNoMoreInteractions(sessionRepository);
    }
}
