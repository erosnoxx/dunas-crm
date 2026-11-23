package com.crm.iam.infrastructure.config;

import com.crm.common.application.cache.Cache;
import com.crm.common.application.email.EmailSenderService;
import com.crm.common.application.events.EventPublisher;
import com.crm.common.application.ratelimit.RateLimiter;
import com.crm.iam.application.IamSettings;
import com.crm.iam.application.contracts.readers.SessionReader;
import com.crm.iam.application.contracts.readers.UserReader;
import com.crm.iam.application.contracts.repositories.OneTimeTokenRepository;
import com.crm.iam.application.contracts.repositories.SessionRepository;
import com.crm.iam.application.contracts.repositories.UserRepository;
import com.crm.iam.application.contracts.services.PasswordHasher;
import com.crm.iam.application.usecases.commands.AcceptInvitation;
import com.crm.iam.application.usecases.commands.AuthenticateSession;
import com.crm.iam.application.usecases.commands.ChangePassword;
import com.crm.iam.application.usecases.commands.ChangeUserRole;
import com.crm.iam.application.usecases.commands.DeactivateUser;
import com.crm.iam.application.usecases.commands.InviteUser;
import com.crm.iam.application.usecases.commands.Login;
import com.crm.iam.application.usecases.commands.Logout;
import com.crm.iam.application.usecases.commands.ReactivateUser;
import com.crm.iam.application.usecases.commands.RequestPasswordReset;
import com.crm.iam.application.usecases.commands.ResendInvitation;
import com.crm.iam.application.usecases.commands.ResetPassword;
import com.crm.iam.application.usecases.commands.RevokeAllSessions;
import com.crm.iam.application.usecases.commands.RevokeSession;
import com.crm.iam.application.usecases.commands.RevokeUserSessions;
import com.crm.iam.application.usecases.queries.GetCurrentUser;
import com.crm.iam.application.usecases.queries.GetUser;
import com.crm.iam.application.usecases.queries.ListSessions;
import com.crm.iam.application.usecases.queries.ListUsers;
import com.crm.iam.infrastructure.email.IamEmailTemplates;
import com.crm.iam.infrastructure.listeners.OneTimeTokenIssuedListener;
import com.crm.iam.infrastructure.listeners.SessionRevokedListener;
import com.crm.iam.infrastructure.listeners.UserDeactivatedListener;
import com.crm.iam.infrastructure.listeners.UserRoleChangedListener;
import com.crm.iam.infrastructure.repositories.OneTimeTokenRepositoryImpl;
import com.crm.iam.infrastructure.readers.SessionReaderImpl;
import com.crm.iam.infrastructure.repositories.SessionRepositoryImpl;
import com.crm.iam.infrastructure.readers.UserReaderImpl;
import com.crm.iam.infrastructure.repositories.UserRepositoryImpl;
import com.crm.iam.infrastructure.persistence.jpa.OneTimeTokenJpaRepository;
import com.crm.iam.infrastructure.mappers.OneTimeTokenMapper;
import com.crm.iam.infrastructure.persistence.jpa.SessionJpaRepository;
import com.crm.iam.infrastructure.mappers.SessionMapper;
import com.crm.iam.infrastructure.persistence.jpa.UserJpaRepository;
import com.crm.iam.infrastructure.mappers.UserMapper;
import com.crm.iam.infrastructure.security.Argon2PasswordHasherImpl;
import com.crm.iam.infrastructure.seed.AdminSeeder;
import jakarta.persistence.EntityManager;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;

/**
 * Registra todos os beans do BC {@code iam} (architecture.md §9): sem {@code @Component} nem
 * {@code @Service}, use case, repository e listener são instanciados aqui, com {@code new}.
 *
 * <p>As peças do kernel (relógio, cache, limite de tentativas, publicador de eventos, e-mail)
 * chegam por parâmetro, e os contratos de {@code iam-application} são devolvidos pelo tipo que os
 * use cases pedem.
 */
@Configuration
@EnableConfigurationProperties(IamProperties.class)
public class IamBeans {

    // --- configuração e serviços técnicos ---------------------------------------------------

    @Bean
    public IamSettings iamSettings(IamProperties properties) {
        return properties.toSettings();
    }

    @Bean
    public PasswordHasher passwordHasher(IamProperties properties) {
        var argon2 = properties.argon2();

        return new Argon2PasswordHasherImpl(
                argon2.memoryKib(),
                argon2.iterations(),
                argon2.parallelism(),
                argon2.saltLength(),
                argon2.hashLength()
        );
    }

    @Bean
    public IamEmailTemplates iamEmailTemplates(IamProperties properties) {
        return new IamEmailTemplates(
                properties.appUrl(),
                properties.invitationValidity(),
                properties.passwordResetValidity()
        );
    }

    // --- persistência -------------------------------------------------------------------------

    @Bean
    public UserMapper userMapper() {
        return new UserMapper();
    }

    @Bean
    public SessionMapper sessionMapper() {
        return new SessionMapper();
    }

    @Bean
    public OneTimeTokenMapper oneTimeTokenMapper() {
        return new OneTimeTokenMapper();
    }

    @Bean
    public UserRepository userRepository(
            UserJpaRepository jpaRepository,
            EntityManager em,
            UserMapper mapper
    ) {
        return new UserRepositoryImpl(
                jpaRepository,
                em,
                mapper
        );
    }

    @Bean
    public SessionRepository sessionRepository(
            SessionJpaRepository jpaRepository,
            EntityManager em,
            SessionMapper mapper
    ) {
        return new SessionRepositoryImpl(
                jpaRepository,
                em,
                mapper
        );
    }

    @Bean
    public OneTimeTokenRepository oneTimeTokenRepository(
            OneTimeTokenJpaRepository jpaRepository,
            EntityManager em,
            OneTimeTokenMapper mapper
    ) {
        return new OneTimeTokenRepositoryImpl(
                jpaRepository,
                em,
                mapper
        );
    }

    @Bean
    public UserReader userReader(EntityManager em) {
        return new UserReaderImpl(em);
    }

    @Bean
    public SessionReader sessionReader(EntityManager em) {
        return new SessionReaderImpl(em);
    }

    // --- commands -----------------------------------------------------------------------------

    @Bean
    public Login login(
            UserRepository userRepository,
            SessionRepository sessionRepository,
            PasswordHasher hasher,
            RateLimiter rateLimiter,
            IamSettings settings,
            Clock clock
    ) {
        return new Login(
                userRepository,
                sessionRepository,
                hasher,
                rateLimiter,
                settings,
                clock
        );
    }

    /** Sem transação própria: a cadeia de segurança o chama a cada requisição (ver o use case). */
    @Bean
    public AuthenticateSession authenticateSession(
            SessionReader sessionReader,
            SessionRepository sessionRepository,
            Cache cache,
            IamSettings settings,
            Clock clock
    ) {
        return new AuthenticateSession(
                sessionReader,
                sessionRepository,
                cache,
                settings,
                clock
        );
    }

    @Bean
    public Logout logout(
            SessionRepository sessionRepository,
            EventPublisher publisher,
            Clock clock
    ) {
        return new Logout(
                sessionRepository,
                publisher,
                clock
        );
    }

    @Bean
    public InviteUser inviteUser(
            UserRepository userRepository,
            OneTimeTokenRepository tokenRepository,
            EventPublisher publisher,
            IamSettings settings,
            Clock clock
    ) {
        return new InviteUser(
                userRepository,
                tokenRepository,
                publisher,
                settings,
                clock
        );
    }

    @Bean
    public ResendInvitation resendInvitation(
            UserRepository userRepository,
            OneTimeTokenRepository tokenRepository,
            EventPublisher publisher,
            IamSettings settings,
            Clock clock
    ) {
        return new ResendInvitation(
                userRepository,
                tokenRepository,
                publisher,
                settings,
                clock
        );
    }

    @Bean
    public AcceptInvitation acceptInvitation(
            OneTimeTokenRepository tokenRepository,
            UserRepository userRepository,
            SessionRepository sessionRepository,
            PasswordHasher hasher,
            IamSettings settings,
            Clock clock
    ) {
        return new AcceptInvitation(
                tokenRepository,
                userRepository,
                sessionRepository,
                hasher,
                settings,
                clock
        );
    }

    @Bean
    public RequestPasswordReset requestPasswordReset(
            UserRepository userRepository,
            OneTimeTokenRepository tokenRepository,
            RateLimiter rateLimiter,
            EventPublisher publisher,
            IamSettings settings,
            Clock clock
    ) {
        return new RequestPasswordReset(
                userRepository,
                tokenRepository,
                rateLimiter,
                publisher,
                settings,
                clock
        );
    }

    @Bean
    public ResetPassword resetPassword(
            OneTimeTokenRepository tokenRepository,
            UserRepository userRepository,
            SessionRepository sessionRepository,
            PasswordHasher hasher,
            EventPublisher publisher,
            Clock clock
    ) {
        return new ResetPassword(
                tokenRepository,
                userRepository,
                sessionRepository,
                hasher,
                publisher,
                clock
        );
    }

    @Bean
    public ChangePassword changePassword(
            UserRepository userRepository,
            SessionRepository sessionRepository,
            PasswordHasher hasher,
            RateLimiter rateLimiter,
            EventPublisher publisher,
            Cache cache,
            IamSettings settings,
            Clock clock
    ) {
        return new ChangePassword(
                userRepository,
                sessionRepository,
                hasher,
                rateLimiter,
                publisher,
                cache,
                settings,
                clock
        );
    }

    @Bean
    public ChangeUserRole changeUserRole(
            UserRepository userRepository,
            EventPublisher publisher,
            Clock clock
    ) {
        return new ChangeUserRole(
                userRepository,
                publisher,
                clock
        );
    }

    @Bean
    public DeactivateUser deactivateUser(
            UserRepository userRepository,
            EventPublisher publisher,
            Clock clock
    ) {
        return new DeactivateUser(
                userRepository,
                publisher,
                clock
        );
    }

    @Bean
    public ReactivateUser reactivateUser(UserRepository userRepository) {
        return new ReactivateUser(userRepository);
    }

    @Bean
    public RevokeSession revokeSession(
            SessionRepository sessionRepository,
            EventPublisher publisher,
            Clock clock
    ) {
        return new RevokeSession(
                sessionRepository,
                publisher,
                clock
        );
    }

    @Bean
    public RevokeAllSessions revokeAllSessions(
            SessionRepository sessionRepository,
            EventPublisher publisher,
            Clock clock
    ) {
        return new RevokeAllSessions(
                sessionRepository,
                publisher,
                clock
        );
    }

    @Bean
    public RevokeUserSessions revokeUserSessions(
            SessionRepository sessionRepository,
            EventPublisher publisher,
            Clock clock
    ) {
        return new RevokeUserSessions(
                sessionRepository,
                publisher,
                clock
        );
    }

    // --- queries ------------------------------------------------------------------------------

    @Bean
    public GetCurrentUser getCurrentUser(UserReader userReader) {
        return new GetCurrentUser(userReader);
    }

    @Bean
    public GetUser getUser(
            UserReader userReader,
            SessionReader sessionReader,
            Clock clock
    ) {
        return new GetUser(
                userReader,
                sessionReader,
                clock
        );
    }

    @Bean
    public ListSessions listSessions(
            SessionReader sessionReader,
            Clock clock
    ) {
        return new ListSessions(sessionReader, clock);
    }

    @Bean
    public ListUsers listUsers(UserReader userReader) {
        return new ListUsers(userReader);
    }

    // --- listeners ----------------------------------------------------------------------------

    @Bean
    public OneTimeTokenIssuedListener oneTimeTokenIssuedListener(
            UserRepository userRepository,
            EmailSenderService emailSender,
            IamEmailTemplates templates
    ) {
        return new OneTimeTokenIssuedListener(
                userRepository,
                emailSender,
                templates
        );
    }

    @Bean
    public SessionRevokedListener sessionRevokedListener(Cache cache) {
        return new SessionRevokedListener(cache);
    }

    /**
     * O template com {@code REQUIRES_NEW} não é um bean: o único {@code TransactionTemplate} de
     * propósito geral do app é o dos use cases, e um segundo por tipo geraria ambiguidade.
     */
    @Bean
    public UserDeactivatedListener userDeactivatedListener(
            RevokeUserSessions revokeUserSessions,
            PlatformTransactionManager transactionManager
    ) {
        var requiresNew = new TransactionTemplate(transactionManager);
        requiresNew.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        return new UserDeactivatedListener(revokeUserSessions, requiresNew);
    }

    @Bean
    public UserRoleChangedListener userRoleChangedListener(
            SessionRepository sessionRepository,
            Cache cache,
            Clock clock
    ) {
        return new UserRoleChangedListener(
                sessionRepository,
                cache,
                clock
        );
    }

    // --- seed ---------------------------------------------------------------------------------

    @Bean
    public AdminSeeder adminSeeder(
            UserRepository userRepository,
            PasswordHasher hasher,
            Clock clock
    ) {
        return new AdminSeeder(
                userRepository,
                hasher,
                clock
        );
    }
}
