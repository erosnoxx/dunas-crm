package com.crm.iam.infrastructure.persistence;

import com.crm.common.application.exceptions.ConflictException;
import com.crm.common.application.pages.Pageable;
import com.crm.iam.application.schemas.UserFilter;
import com.crm.iam.domain.entities.OneTimeToken;
import com.crm.iam.domain.entities.Session;
import com.crm.iam.domain.entities.User;
import com.crm.iam.domain.vos.DeviceLabel;
import com.crm.iam.domain.vos.PasswordHash;
import com.crm.iam.domain.vos.Role;
import com.crm.iam.domain.vos.TokenPurpose;
import com.crm.iam.domain.vos.TokenSecret;
import com.crm.iam.domain.vos.UserName;
import com.crm.iam.domain.vos.UserStatus;
import com.crm.shared.domain.vos.contact.Email;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Persistência do IAM contra o Postgres real (Testcontainers; exige Docker). Sobe com
 * {@code ddl-auto=validate}: se a migration e as entities divergirem, o contexto nem carrega.
 */
@SpringBootTest(
        classes = IamPersistencePostgresTest.TestApplication.class,
        properties = {
                "spring.flyway.locations=classpath:db/migration/iam",
                "spring.flyway.table=flyway_schema_history_iam",
                "spring.jpa.hibernate.ddl-auto=validate"
        }
)
@DisplayName("Persistência do IAM contra o Postgres")
class IamPersistencePostgresTest {

    private static final Instant NOW = Instant.parse("2026-11-19T12:00:00Z");

    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16");

    static {
        POSTGRES.start();
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EntityScan(basePackageClasses = UserEntity.class)
    @EnableJpaRepositories(basePackageClasses = UserJpaRepository.class)
    static class TestApplication {

        @Bean
        Clock clock() {
            return Clock.fixed(NOW, ZoneOffset.UTC);
        }

        @Bean
        JpaUserRepositoryImpl userRepository(UserJpaRepository jpa, EntityManager em) {
            return new JpaUserRepositoryImpl(jpa, em, new UserMapper());
        }

        @Bean
        JpaOneTimeTokenRepositoryImpl tokenRepository(OneTimeTokenJpaRepository jpa, EntityManager em) {
            return new JpaOneTimeTokenRepositoryImpl(jpa, em, new OneTimeTokenMapper());
        }

        @Bean
        JpaSessionRepositoryImpl sessionRepository(SessionJpaRepository jpa, EntityManager em) {
            return new JpaSessionRepositoryImpl(jpa, em, new SessionMapper());
        }

        @Bean
        JpaUserReaderImpl userReader(EntityManager em) {
            return new JpaUserReaderImpl(em);
        }

        @Bean
        JpaSessionReaderImpl sessionReader(EntityManager em) {
            return new JpaSessionReaderImpl(em);
        }
    }

    @Autowired
    private JpaUserRepositoryImpl users;

    @Autowired
    private JpaOneTimeTokenRepositoryImpl tokens;

    @Autowired
    private JpaSessionRepositoryImpl sessions;

    @Autowired
    private JpaUserReaderImpl userReader;

    @Autowired
    private JpaSessionReaderImpl sessionReader;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void cleanTables() {
        jdbc.execute("DELETE FROM sessions");
        jdbc.execute("DELETE FROM one_time_tokens");
        jdbc.execute("DELETE FROM users");
    }

    private User savedUser(String name, String email, Role role) {
        return users.save(User.invite(UserName.of(name), Email.of(email), role, NOW));
    }

    private User savedActiveUser(String name, String email, Role role) {
        var user = users.save(User.invite(UserName.of(name), Email.of(email), role, NOW));

        user.acceptInvitation(PasswordHash.of("$argon2id$v=19$m=19456,t=2,p=1$salt$hash"));

        return users.save(user);
    }

    private Session savedSession(UUID userId, Instant startedAt, Duration validity) {
        return sessions.save(Session.start(
                userId,
                TokenSecret.generate(),
                DeviceLabel.of("Galaxy A54, app DUNAS"),
                validity,
                startedAt
        ));
    }

    @Test
    @DisplayName("grava o usuário, devolve a versão e deixa updated_at nulo no primeiro insert")
    void savesUserWithVersionAndNullUpdatedAt() {
        var saved = savedUser("Maria Souza", "maria@empresa.com", Role.COMMERCIAL);

        assertThat(saved.getVersion()).contains(0L);

        var updatedAt = jdbc.queryForObject("SELECT updated_at FROM users WHERE id = ?", Object.class, saved.getId());

        assertThat(updatedAt).isNull();
    }

    @Test
    @DisplayName("e-mail repetido, mesmo em caixa diferente, vira ConflictException")
    void duplicateEmailInOtherCaseIsConflict() {
        savedUser("Joao Lima", "Joao@Empresa.com", Role.TECHNICIAN);

        assertThatThrownBy(() -> savedUser("Outro Joao", "joao@empresa.com", Role.COMMERCIAL))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    @DisplayName("findByEmail acha o usuário pelo e-mail normalizado e devolve vazio se não existe")
    void findsByEmail() {
        var saved = savedUser("Joao Lima", "Joao@Empresa.com", Role.TECHNICIAN);

        assertThat(users.findByEmail(Email.of("JOAO@empresa.com"))).get()
                .extracting(User::getId)
                .isEqualTo(saved.getId());
        assertThat(users.findByEmail(Email.of("ninguem@empresa.com"))).isEmpty();
    }

    @Test
    @DisplayName("findActiveByUserId ignora a sessão revogada e a expirada")
    void activeSessionsIgnoreRevokedAndExpired() {
        var user = savedUser("Ana Paula", "ana@empresa.com", Role.ADMINISTRATOR);
        var active = savedSession(user.getId(), NOW, Duration.ofDays(30));
        var expired = savedSession(user.getId(), NOW.minus(Duration.ofDays(40)), Duration.ofDays(30));
        var revoked = savedSession(user.getId(), NOW, Duration.ofDays(30));

        revoked.revoke(user.getId(), NOW);
        sessions.save(revoked);

        var found = sessions.findActiveByUserId(user.getId(), NOW);

        assertThat(found).extracting(Session::getId).containsExactly(active.getId());
        assertThat(found).extracting(Session::getId).doesNotContain(expired.getId(), revoked.getId());
    }

    @Test
    @DisplayName("findByTokenHash acha a sessão e o token pelo hash")
    void findsByTokenHash() {
        var user = savedUser("Ana Paula", "ana@empresa.com", Role.ADMINISTRATOR);
        var session = savedSession(user.getId(), NOW, Duration.ofDays(30));
        var token = tokens.save(OneTimeToken.issue(user.getId(), TokenPurpose.INVITATION, TokenSecret.generate(), Duration.ofHours(48), NOW));

        assertThat(sessions.findByTokenHash(session.getTokenHash())).get().extracting(Session::getId).isEqualTo(session.getId());
        assertThat(tokens.findByTokenHash(token.getTokenHash())).get().extracting(OneTimeToken::getId).isEqualTo(token.getId());
    }

    @Test
    @DisplayName("search filtra por papel, por situação e por texto, sem diferenciar caixa")
    void searchFiltersByRoleStatusAndText() {
        savedUser("Maria Souza", "maria@empresa.com", Role.COMMERCIAL);
        savedActiveUser("Carlos Técnico", "carlos@campo.com", Role.TECHNICIAN);
        savedActiveUser("Paula Admin", "paula@empresa.com", Role.ADMINISTRATOR);

        var all = Pageable.of(0, 10);

        assertThat(userReader.search(new UserFilter(Role.TECHNICIAN, null, null), all).content())
                .extracting(summary -> summary.name()).containsExactly("Carlos Técnico");
        assertThat(userReader.search(new UserFilter(null, UserStatus.INVITED, null), all).content())
                .extracting(summary -> summary.name()).containsExactly("Maria Souza");
        assertThat(userReader.search(new UserFilter(null, null, "CAMPO"), all).content())
                .extracting(summary -> summary.name()).containsExactly("Carlos Técnico");
        assertThat(userReader.search(new UserFilter(null, null, "paula"), all).content())
                .extracting(summary -> summary.name()).containsExactly("Paula Admin");
        assertThat(userReader.search(new UserFilter(null, null, "@empresa"), all).totalElements()).isEqualTo(2);
        assertThat(userReader.search(new UserFilter(null, null, "  "), all).totalElements()).isEqualTo(3);
    }

    @Test
    @DisplayName("search trata % e _ do texto como caracteres literais")
    void searchEscapesLikeWildcards() {
        savedUser("Ana 100% Solar", "ana@empresa.com", Role.COMMERCIAL);
        savedUser("Bruno Silva", "bruno@empresa.com", Role.COMMERCIAL);
        savedUser("Carla_Dias", "carla@empresa.com", Role.COMMERCIAL);
        savedUser("Carlo Dias", "carlo@empresa.com", Role.COMMERCIAL);

        var all = Pageable.of(0, 10);

        assertThat(userReader.search(new UserFilter(null, null, "%"), all).content())
                .extracting(summary -> summary.name()).containsExactly("Ana 100% Solar");
        assertThat(userReader.search(new UserFilter(null, null, "_"), all).content())
                .extracting(summary -> summary.name()).containsExactly("Carla_Dias");
        assertThat(userReader.search(new UserFilter(null, null, "carl_ dias"), all).content()).isEmpty();
    }

    @Test
    @DisplayName("search pagina e informa o total")
    void searchPaginates() {
        for (var i = 1; i <= 5; i++)
            savedUser("Usuario " + i, "u" + i + "@empresa.com", Role.COMMERCIAL);

        var first = userReader.search(new UserFilter(null, null, null), Pageable.of(0, 2, "name", "ASC"));
        var last = userReader.search(new UserFilter(null, null, null), Pageable.of(2, 2, "name", "ASC"));

        assertThat(first.content()).extracting(summary -> summary.name()).containsExactly("Usuario 1", "Usuario 2");
        assertThat(first.totalElements()).isEqualTo(5);
        assertThat(first.totalPages()).isEqualTo(3);
        assertThat(last.content()).extracting(summary -> summary.name()).containsExactly("Usuario 5");
    }

    @Test
    @DisplayName("lastUsedAt é o maior uso entre as sessões e nulo se nunca entrou")
    void lastUsedAtIsTheLatestSession() {
        var withSessions = savedUser("Com Sessao", "com@empresa.com", Role.COMMERCIAL);
        var neverLoggedIn = savedUser("Sem Sessao", "sem@empresa.com", Role.COMMERCIAL);
        var older = Instant.parse("2026-11-01T08:00:00Z");
        var newer = Instant.parse("2026-11-10T09:30:00Z");

        savedSession(withSessions.getId(), older, Duration.ofDays(30));
        savedSession(withSessions.getId(), newer, Duration.ofDays(30));

        assertThat(userReader.findSummary(withSessions.getId())).get()
                .extracting(summary -> summary.lastUsedAt()).isEqualTo(newer);
        assertThat(userReader.findSummary(neverLoggedIn.getId())).get()
                .extracting(summary -> summary.lastUsedAt()).isNull();
        assertThat(userReader.findSummary(UUID.randomUUID())).isEmpty();

        var page = userReader.search(new UserFilter(null, null, null), Pageable.of(0, 10, "name", "ASC"));

        assertThat(page.content()).extracting(summary -> summary.lastUsedAt()).containsExactly(newer, null);
    }

    @Test
    @DisplayName("findActiveByUser lista as sessões ativas e marca a corrente")
    void listsActiveSessionsMarkingCurrent() {
        var user = savedUser("Ana Paula", "ana@empresa.com", Role.ADMINISTRATOR);
        var current = savedSession(user.getId(), NOW, Duration.ofDays(30));
        var other = savedSession(user.getId(), NOW.minusSeconds(3600), Duration.ofDays(30));
        savedSession(user.getId(), NOW.minus(Duration.ofDays(40)), Duration.ofDays(30));

        var found = sessionReader.findActiveByUser(user.getId(), current.getId(), NOW);

        assertThat(found).hasSize(2);
        assertThat(found).filteredOn(summary -> summary.current()).extracting(summary -> summary.id()).containsExactly(current.getId());
        assertThat(found).filteredOn(summary -> !summary.current()).extracting(summary -> summary.id()).containsExactly(other.getId());
    }

    @Test
    @DisplayName("findPrincipal junta sessão e usuário numa leitura só")
    void findsPrincipalWithSessionAndUser() {
        var user = savedActiveUser("Ana Paula", "ana@empresa.com", Role.ADMINISTRATOR);
        var session = savedSession(user.getId(), NOW, Duration.ofDays(30));

        var principal = sessionReader.findPrincipal(session.getTokenHash()).orElseThrow();

        assertThat(principal.sessionId()).isEqualTo(session.getId());
        assertThat(principal.userId()).isEqualTo(user.getId());
        assertThat(principal.role()).isEqualTo(Role.ADMINISTRATOR);
        assertThat(principal.userStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(principal.mustChangePassword()).isFalse();
        assertThat(principal.lastUsedAt()).isEqualTo(NOW);
        assertThat(principal.expiresAt()).isEqualTo(NOW.plus(Duration.ofDays(30)));
        assertThat(principal.revokedAt()).isNull();
        assertThat(sessionReader.findPrincipal(TokenSecret.generate().hash())).isEmpty();
    }

    @Test
    @DisplayName("findPending devolve só os links ainda não usados nem substituídos")
    void findsPendingTokens() {
        var user = savedUser("Ana Paula", "ana@empresa.com", Role.ADMINISTRATOR);
        var pending = tokens.save(OneTimeToken.issue(user.getId(), TokenPurpose.PASSWORD_RESET, TokenSecret.generate(), Duration.ofHours(1), NOW));
        var invalidated = OneTimeToken.issue(user.getId(), TokenPurpose.PASSWORD_RESET, TokenSecret.generate(), Duration.ofHours(1), NOW);
        var otherPurpose = tokens.save(OneTimeToken.issue(user.getId(), TokenPurpose.INVITATION, TokenSecret.generate(), Duration.ofHours(48), NOW));

        invalidated.invalidate(NOW);
        tokens.save(invalidated);

        assertThat(tokens.findPending(user.getId(), TokenPurpose.PASSWORD_RESET)).extracting(OneTimeToken::getId).containsExactly(pending.getId());
        assertThat(tokens.findPending(user.getId(), TokenPurpose.INVITATION)).extracting(OneTimeToken::getId).containsExactly(otherPurpose.getId());
    }

    @Test
    @DisplayName("consume grava o uso uma vez só; link já consumido ou substituído devolve false")
    void consumeWritesOnce() throws Exception {
        var user = savedUser("Ana Paula", "ana@empresa.com", Role.ADMINISTRATOR);
        var token = tokens.save(OneTimeToken.issue(user.getId(), TokenPurpose.INVITATION, TokenSecret.generate(), Duration.ofHours(48), NOW));
        var invalidated = OneTimeToken.issue(user.getId(), TokenPurpose.INVITATION, TokenSecret.generate(), Duration.ofHours(48), NOW);

        invalidated.invalidate(NOW);
        invalidated = tokens.save(invalidated);

        token.consume(NOW);
        assertThat(tokens.consume(token)).isTrue();
        assertThat(tokens.consume(token)).isFalse();

        var stored = tokens.findById(token.getId()).orElseThrow();

        assertThat(stored.getUsedAt()).contains(NOW);
        assertThat(stored.getVersion()).contains(1L);

        var reloaded = tokens.findById(invalidated.getId()).orElseThrow();

        assertThat(reloaded.getInvalidatedAt()).isPresent();
        assertThat(tokens.consume(OneTimeToken.reconstitute(
                reloaded.getId(),
                reloaded.getVersion().orElse(null),
                reloaded.getUserId(),
                reloaded.getPurpose(),
                reloaded.getTokenHash(),
                reloaded.getExpiresAt(),
                NOW,
                reloaded.getInvalidatedAt().orElse(null)
        ))).isFalse();
    }

    @Test
    @DisplayName("consume chamado por duas threads sobre o mesmo token: exatamente uma recebe true")
    void concurrentConsumeHasExactlyOneWinner() throws Exception {
        var user = savedUser("Ana Paula", "ana@empresa.com", Role.ADMINISTRATOR);
        var saved = tokens.save(OneTimeToken.issue(user.getId(), TokenPurpose.PASSWORD_RESET, TokenSecret.generate(), Duration.ofHours(1), NOW));
        var attempts = 8;
        var ready = new CountDownLatch(attempts);
        var go = new CountDownLatch(1);
        var pool = Executors.newFixedThreadPool(attempts);

        try {
            var futures = new ArrayList<java.util.concurrent.Future<Boolean>>();

            for (var i = 0; i < attempts; i++) {
                var copy = tokens.findById(saved.getId()).orElseThrow();

                copy.consume(NOW);

                Callable<Boolean> attempt = () -> {
                    ready.countDown();
                    go.await();

                    return tokens.consume(copy);
                };

                futures.add(pool.submit(attempt));
            }

            ready.await();
            go.countDown();

            List<Boolean> results = new ArrayList<>();

            for (var future : futures)
                results.add(future.get());

            assertThat(results).filteredOn(won -> won).hasSize(1);
            assertThat(results).filteredOn(won -> !won).hasSize(attempts - 1);
        }
        finally {
            pool.shutdownNow();
        }
    }
}
