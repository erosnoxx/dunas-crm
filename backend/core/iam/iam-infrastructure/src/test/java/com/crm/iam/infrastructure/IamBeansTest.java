package com.crm.iam.infrastructure;

import com.crm.common.application.annotations.UseCase;
import com.crm.common.application.cache.Cache;
import com.crm.common.application.email.EmailSenderService;
import com.crm.common.application.events.EventPublisher;
import com.crm.common.application.ratelimit.RateLimiter;
import com.crm.iam.application.contracts.readers.SessionReader;
import com.crm.iam.application.contracts.readers.UserReader;
import com.crm.iam.application.contracts.repositories.OneTimeTokenRepository;
import com.crm.iam.application.contracts.repositories.SessionRepository;
import com.crm.iam.application.contracts.repositories.UserRepository;
import com.crm.iam.infrastructure.persistence.OneTimeTokenJpaRepository;
import com.crm.iam.infrastructure.persistence.SessionJpaRepository;
import com.crm.iam.infrastructure.persistence.UserJpaRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.transaction.PlatformTransactionManager;

import java.lang.reflect.Method;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Monta os beans do IAM à mão, sem subir o Spring: prova que cada use case, listener e o seeder
 * constroem com as dependências que a {@link IamBeans} declara. O boot de ponta a ponta é da
 * tarefa de integração.
 */
@DisplayName("IamBeans")
class IamBeansTest {

    private final IamBeans beans = new IamBeans();

    private IamProperties properties;
    private Clock clock;
    private UserRepository userRepository;
    private SessionRepository sessionRepository;
    private OneTimeTokenRepository tokenRepository;
    private UserReader userReader;
    private SessionReader sessionReader;
    private EventPublisher publisher;
    private Cache cache;
    private RateLimiter rateLimiter;

    @BeforeEach
    void setUp() {
        // custo mínimo: o Login calcula um hash na construção
        properties = new Binder(new MapConfigurationPropertySource(Map.of(
                "crm.iam.argon2.memory-kib", "1024",
                "crm.iam.argon2.iterations", "1"
        ))).bindOrCreate("crm.iam", IamProperties.class);

        clock = Clock.fixed(Instant.parse("2026-11-19T12:00:00Z"), ZoneOffset.UTC);

        var em = mock(EntityManager.class);

        userRepository = beans.userRepository(mock(UserJpaRepository.class), em, beans.userMapper());
        sessionRepository = beans.sessionRepository(mock(SessionJpaRepository.class), em, beans.sessionMapper());
        tokenRepository = beans.oneTimeTokenRepository(
                mock(OneTimeTokenJpaRepository.class), em, beans.oneTimeTokenMapper());
        userReader = beans.userReader(em);
        sessionReader = beans.sessionReader(em);
        publisher = mock(EventPublisher.class);
        cache = mock(Cache.class);
        rateLimiter = mock(RateLimiter.class);
    }

    @Test
    @DisplayName("repositories e readers são montados")
    void persistenceBeansAreBuilt() {
        assertThat(userRepository).isNotNull();
        assertThat(sessionRepository).isNotNull();
        assertThat(tokenRepository).isNotNull();
        assertThat(userReader).isNotNull();
        assertThat(sessionReader).isNotNull();
    }

    @Test
    @DisplayName("todos os use cases de commands e queries constroem")
    void everyUseCaseBuilds() {
        var settings = beans.iamSettings(properties);
        var hasher = beans.passwordHasher(properties);

        assertThat(settings).isNotNull();

        assertThat(beans.login(userRepository, sessionRepository, hasher, rateLimiter, settings, clock)).isNotNull();
        assertThat(beans.authenticateSession(sessionReader, sessionRepository, cache, settings, clock)).isNotNull();
        assertThat(beans.logout(sessionRepository, publisher, clock)).isNotNull();
        assertThat(beans.inviteUser(userRepository, tokenRepository, publisher, settings, clock)).isNotNull();
        assertThat(beans.resendInvitation(userRepository, tokenRepository, publisher, settings, clock)).isNotNull();
        assertThat(beans.acceptInvitation(
                tokenRepository, userRepository, sessionRepository, hasher, settings, clock)).isNotNull();
        assertThat(beans.requestPasswordReset(
                userRepository, tokenRepository, rateLimiter, publisher, settings, clock)).isNotNull();
        assertThat(beans.resetPassword(
                tokenRepository, userRepository, sessionRepository, hasher, publisher, clock)).isNotNull();
        assertThat(beans.changePassword(
                userRepository, sessionRepository, hasher, rateLimiter, publisher, cache, settings, clock)).isNotNull();
        assertThat(beans.changeUserRole(userRepository, publisher, clock)).isNotNull();
        assertThat(beans.deactivateUser(userRepository, publisher, clock)).isNotNull();
        assertThat(beans.reactivateUser(userRepository)).isNotNull();
        assertThat(beans.revokeSession(sessionRepository, publisher, clock)).isNotNull();
        assertThat(beans.revokeAllSessions(sessionRepository, publisher, clock)).isNotNull();
        assertThat(beans.revokeUserSessions(sessionRepository, publisher, clock)).isNotNull();

        assertThat(beans.getCurrentUser(userReader)).isNotNull();
        assertThat(beans.getUser(userReader, sessionReader, clock)).isNotNull();
        assertThat(beans.listSessions(sessionReader, clock)).isNotNull();
        assertThat(beans.listUsers(userReader)).isNotNull();
    }

    @Test
    @DisplayName("listeners, templates e seeder constroem")
    void listenersAndSeederBuild() {
        var hasher = beans.passwordHasher(properties);
        var revokeUserSessions = beans.revokeUserSessions(sessionRepository, publisher, clock);

        assertThat(beans.oneTimeTokenIssuedListener(
                userRepository, mock(EmailSenderService.class), beans.iamEmailTemplates(properties))).isNotNull();
        assertThat(beans.sessionRevokedListener(cache)).isNotNull();
        assertThat(beans.userDeactivatedListener(revokeUserSessions, mock(PlatformTransactionManager.class)))
                .isNotNull();
        assertThat(beans.userRoleChangedListener(sessionRepository, cache, clock)).isNotNull();
        assertThat(beans.adminSeeder(userRepository, hasher, clock)).isNotNull();
    }

    @Test
    @DisplayName("todo use case de iam-application tem um @Bean na IamBeans")
    void everyUseCaseClassHasABeanMethod() {
        var scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(UseCase.class));

        Set<String> useCases = scanner.findCandidateComponents("com.crm.iam.application").stream()
                .map(definition -> definition.getBeanClassName())
                .collect(Collectors.toSet());

        Set<String> registered = Arrays.stream(IamBeans.class.getDeclaredMethods())
                .filter(method -> method.isAnnotationPresent(Bean.class))
                .map(Method::getReturnType)
                .map(Class::getName)
                .collect(Collectors.toSet());

        assertThat(useCases).isNotEmpty();
        assertThat(registered).containsAll(useCases);
    }
}
