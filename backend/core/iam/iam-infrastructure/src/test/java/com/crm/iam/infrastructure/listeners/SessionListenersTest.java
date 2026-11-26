package com.crm.iam.infrastructure.listeners;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.crm.common.application.cache.Cache;
import com.crm.common.application.events.EventPublisher;
import com.crm.iam.application.contracts.repositories.SessionRepository;
import com.crm.iam.application.usecases.commands.RevokeUserSessions;
import com.crm.iam.domain.entities.Session;
import com.crm.iam.domain.events.SessionRevoked;
import com.crm.iam.domain.events.UserDeactivated;
import com.crm.iam.domain.events.UserRoleChanged;
import com.crm.iam.domain.vos.DeviceLabel;
import com.crm.iam.domain.vos.TokenSecret;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@DisplayName("Listeners de sessão do IAM")
class SessionListenersTest {

    private static final Instant NOW = Instant.parse("2026-11-19T12:00:00Z");

    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    private final UUID userId = UUID.randomUUID();

    private Cache cache;
    private SessionRepository sessionRepository;
    private EventPublisher publisher;
    private Logger rootLogger;
    private ListAppender<ILoggingEvent> appender;

    @BeforeEach
    void setUp() {
        rootLogger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        appender = new ListAppender<>();
        appender.start();
        rootLogger.addAppender(appender);

        cache = mock(Cache.class);
        sessionRepository = mock(SessionRepository.class);
        publisher = mock(EventPublisher.class);
    }

    @AfterEach
    void tearDown() {
        rootLogger.detachAppender(appender);
    }

    private Session session() {
        return Session.start(userId, TokenSecret.generate(), DeviceLabel.of("Chrome"), Duration.ofDays(30), NOW);
    }

    @Test
    @DisplayName("SessionRevoked remove exatamente a chave iam:session:<hash> do cache")
    void sessionRevokedEvictsExactlyTheSessionKey() {
        var session = session();
        var listener = new SessionRevokedListener(cache);

        listener.on(new SessionRevoked(session.getId(), NOW, session.getTokenHash()));

        verify(cache).evict("iam:session:" + session.getTokenHash().getValue());
        verifyNoMoreInteractions(cache);
    }

    @Test
    @DisplayName("UserRoleChanged remove do cache a chave de cada sessão ativa do usuário")
    void roleChangedEvictsEveryActiveSessionKey() {
        var first = session();
        var second = session();
        when(sessionRepository.findActiveByUserId(userId, NOW)).thenReturn(List.of(first, second));
        var listener = new UserRoleChangedListener(sessionRepository, cache, CLOCK);

        listener.on(new UserRoleChanged(userId, NOW));

        verify(cache).evict("iam:session:" + first.getTokenHash().getValue());
        verify(cache).evict("iam:session:" + second.getTokenHash().getValue());
        verifyNoMoreInteractions(cache);
    }

    @Test
    @DisplayName("UserRoleChanged de usuário sem sessão ativa não mexe no cache")
    void roleChangedWithoutSessionsLeavesTheCacheAlone() {
        when(sessionRepository.findActiveByUserId(userId, NOW)).thenReturn(List.of());
        var listener = new UserRoleChangedListener(sessionRepository, cache, CLOCK);

        listener.on(new UserRoleChanged(userId, NOW));

        verify(cache, never()).evict(any());
    }

    @Test
    @DisplayName("UserDeactivated revoga todas as sessões ativas, sem autor, e a revogação publica SessionRevoked")
    void deactivatedRevokesEveryActiveSession() {
        var first = session();
        var second = session();
        when(sessionRepository.findActiveByUserId(userId, NOW)).thenReturn(List.of(first, second));

        var listener = deactivatedListener(transactionManager());

        listener.on(new UserDeactivated(userId, NOW));

        assertThat(first.getRevokedAt()).contains(NOW);
        assertThat(first.getRevokedBy()).isEmpty();
        assertThat(second.getRevokedAt()).contains(NOW);

        verify(sessionRepository).save(first);
        verify(sessionRepository).save(second);
        verify(publisher, times(2)).publishAll(any());
    }

    @Test
    @DisplayName("UserDeactivated roda numa transação nova: a original já commitou quando o listener é chamado")
    void deactivatedRunsInANewTransaction() {
        var manager = mock(PlatformTransactionManager.class);
        var status = new SimpleTransactionStatus();
        when(manager.getTransaction(any())).thenReturn(status);
        when(sessionRepository.findActiveByUserId(userId, NOW)).thenReturn(List.of(session()));

        deactivatedListener(manager).on(new UserDeactivated(userId, NOW));

        var definition = ArgumentCaptor.forClass(TransactionDefinition.class);
        var order = inOrder(manager, sessionRepository);

        order.verify(manager).getTransaction(definition.capture());
        order.verify(sessionRepository).save(any());
        order.verify(manager).commit(status);

        assertThat(definition.getValue().getPropagationBehavior())
                .isEqualTo(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @Test
    @DisplayName("UserDeactivated com falha na revogação não propaga para quem publicou")
    void deactivatedFailureDoesNotPropagate() {
        when(sessionRepository.findActiveByUserId(userId, NOW)).thenThrow(new IllegalStateException("banco fora"));

        var listener = deactivatedListener(transactionManager());

        listener.on(new UserDeactivated(userId, NOW));

        verify(sessionRepository, never()).save(any());
        assertThat(appender.list).anySatisfy(logged -> {
            assertThat(logged.getLevel()).isEqualTo(Level.ERROR);
            assertThat(logged.getThrowableProxy().getMessage()).isEqualTo("banco fora");
        });
    }

    @Test
    @DisplayName("UserRoleChanged com falha ao buscar as sessões não propaga e é logado com a exception")
    void roleChangedFailureDoesNotPropagate() {
        when(sessionRepository.findActiveByUserId(userId, NOW)).thenThrow(new IllegalStateException("banco fora"));
        var listener = new UserRoleChangedListener(sessionRepository, cache, CLOCK);

        listener.on(new UserRoleChanged(userId, NOW));

        verify(cache, never()).evict(any());
        assertThat(appender.list).anySatisfy(logged -> {
            assertThat(logged.getLevel()).isEqualTo(Level.ERROR);
            assertThat(logged.getThrowableProxy().getMessage()).isEqualTo("banco fora");
        });
    }

    private static PlatformTransactionManager transactionManager() {
        var manager = mock(PlatformTransactionManager.class);

        when(manager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());

        return manager;
    }

    private UserDeactivatedListener deactivatedListener(PlatformTransactionManager manager) {
        var requiresNew = new TransactionTemplate(manager);
        requiresNew.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        return new UserDeactivatedListener(
                new RevokeUserSessions(sessionRepository, publisher, CLOCK),
                requiresNew
        );
    }
}
