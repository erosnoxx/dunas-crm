package com.crm.iam.application.usecases.commands;

import com.crm.common.application.cache.Cache;
import com.crm.common.application.exceptions.UnauthorizedException;
import com.crm.iam.application.Actor;
import com.crm.iam.application.contracts.readers.SessionReader;
import com.crm.iam.application.contracts.repositories.SessionRepository;
import com.crm.iam.application.schemas.SessionPrincipal;
import com.crm.iam.domain.entities.Session;
import com.crm.iam.domain.vos.DeviceLabel;
import com.crm.iam.domain.vos.Role;
import com.crm.iam.domain.vos.TokenHash;
import com.crm.iam.domain.vos.TokenSecret;
import com.crm.iam.domain.vos.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import static com.crm.iam.application.usecases.commands.IamFixtures.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@DisplayName("AuthenticateSession")
class AuthenticateSessionTest {

    private SessionReader sessionReader;
    private SessionRepository sessionRepository;
    private Cache cache;
    private AuthenticateSession authenticateSession;

    private final TokenSecret secret = TokenSecret.generate();
    private final TokenHash tokenHash = secret.hash();
    private final String key = "iam:session:" + tokenHash.getValue();
    private final UUID userId = UUID.randomUUID();
    private final UUID sessionId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        sessionReader = mock(SessionReader.class);
        sessionRepository = mock(SessionRepository.class);
        cache = mock(Cache.class);

        authenticateSession = new AuthenticateSession(sessionReader, sessionRepository, cache, SETTINGS, CLOCK);
    }

    private SessionPrincipal principal(
            UserStatus status,
            Instant lastUsedAt,
            Instant expiresAt,
            Instant revokedAt
    ) {
        return new SessionPrincipal(sessionId, userId, Role.ADMINISTRATOR, status, false, lastUsedAt, expiresAt, revokedAt);
    }

    private SessionPrincipal valid(Instant lastUsedAt) {
        return principal(UserStatus.ACTIVE, lastUsedAt, NOW.plus(Duration.ofDays(29)), null);
    }

    /** Cache sem a chave (ou fora do ar, que pelo contrato é a mesma coisa): executa o loader. */
    private void cacheMiss() {
        when(cache.getOrLoad(eq(key), eq(SessionPrincipal.class), eq(SETTINGS.sessionCacheTtl()), any()))
                .thenAnswer(invocation -> ((Supplier<?>) invocation.getArgument(3)).get());
    }

    private void cacheHit(SessionPrincipal principal) {
        when(cache.getOrLoad(eq(key), eq(SessionPrincipal.class), eq(SETTINGS.sessionCacheTtl()), any()))
                .thenReturn(principal);
    }

    private Session storedSession(
            Instant lastUsedAt,
            Instant revokedAt
    ) {
        return Session.reconstitute(
                sessionId,
                1L,
                userId,
                tokenHash,
                DeviceLabel.of("Galaxy A54"),
                NOW.minus(Duration.ofDays(1)),
                lastUsedAt,
                NOW.plus(Duration.ofDays(29)),
                revokedAt,
                revokedAt == null ? null : userId
        );
    }

    @Test
    @DisplayName("usa o cache com a chave iam:session:<hash> e o TTL do IamSettings e devolve o Actor")
    void authenticatesThroughTheCache() {
        cacheHit(valid(NOW.minus(Duration.ofMinutes(1))));

        var result = authenticateSession.execute(secret.getValue());

        assertThat(result.actor()).isEqualTo(new Actor(userId, Role.ADMINISTRATOR, sessionId));
        assertThat(result.mustChangePassword()).isFalse();
        verify(cache).getOrLoad(eq(key), eq(SessionPrincipal.class), eq(Duration.ofSeconds(60)), any());
        verifyNoInteractions(sessionReader);
    }

    @Test
    @DisplayName("devolve mustChangePassword da sessão")
    void returnsMustChangePassword() {
        cacheHit(new SessionPrincipal(
                sessionId, userId, Role.COMMERCIAL, UserStatus.ACTIVE, true,
                NOW, NOW.plus(Duration.ofDays(30)), null));

        assertThat(authenticateSession.execute(secret.getValue()).mustChangePassword()).isTrue();
    }

    @Test
    @DisplayName("sem a chave no cache, o loader lê do SessionReader (é o que acontece com o Redis fora do ar)")
    void fallsBackToTheReaderWhenTheCacheDoesNotAnswer() {
        cacheMiss();
        when(sessionReader.findPrincipal(tokenHash)).thenReturn(Optional.of(valid(NOW)));

        var result = authenticateSession.execute(secret.getValue());

        assertThat(result.actor().sessionId()).isEqualTo(sessionId);
        verify(sessionReader).findPrincipal(tokenHash);
    }

    @Test
    @DisplayName("token desconhecido é session_invalid")
    void unknownToken() {
        cacheMiss();
        when(sessionReader.findPrincipal(tokenHash)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authenticateSession.execute(secret.getValue()))
                .isInstanceOf(UnauthorizedException.class)
                .satisfies(applicationCode("session_invalid"));
    }

    @Test
    @DisplayName("sessão revogada é session_invalid")
    void revokedSession() {
        cacheHit(principal(UserStatus.ACTIVE, NOW, NOW.plus(Duration.ofDays(29)), NOW.minusSeconds(30)));

        assertThatThrownBy(() -> authenticateSession.execute(secret.getValue()))
                .isInstanceOf(UnauthorizedException.class)
                .satisfies(applicationCode("session_invalid"));
    }

    @Test
    @DisplayName("sessão expirada é session_invalid")
    void expiredSession() {
        cacheHit(principal(UserStatus.ACTIVE, NOW.minus(Duration.ofDays(31)), NOW, null));

        assertThatThrownBy(() -> authenticateSession.execute(secret.getValue()))
                .isInstanceOf(UnauthorizedException.class)
                .satisfies(applicationCode("session_invalid"));
    }

    @ParameterizedTest
    @EnumSource(value = UserStatus.class, names = "ACTIVE", mode = EnumSource.Mode.EXCLUDE)
    @DisplayName("usuário que não está ativo é session_invalid")
    void inactiveUser(UserStatus status) {
        cacheHit(principal(status, NOW, NOW.plus(Duration.ofDays(29)), null));

        assertThatThrownBy(() -> authenticateSession.execute(secret.getValue()))
                .isInstanceOf(UnauthorizedException.class)
                .satisfies(applicationCode("session_invalid"));
    }

    @Test
    @DisplayName("último uso há 4 minutos não carrega nem grava a sessão")
    void doesNotRenewBeforeTheInterval() {
        cacheHit(valid(NOW.minus(Duration.ofMinutes(4))));

        authenticateSession.execute(secret.getValue());

        verifyNoInteractions(sessionRepository);
    }

    @Test
    @DisplayName("último uso há 6 minutos carrega a sessão, renova e grava uma vez")
    void renewsAfterTheInterval() {
        var stored = storedSession(NOW.minus(Duration.ofMinutes(6)), null);
        cacheHit(valid(NOW.minus(Duration.ofMinutes(6))));
        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(stored));

        authenticateSession.execute(secret.getValue());

        assertThat(stored.getLastUsedAt()).isEqualTo(NOW);
        assertThat(stored.getExpiresAt()).isEqualTo(NOW.plus(SETTINGS.sessionValidity()));
        verify(sessionRepository).save(stored);
    }

    @Test
    @DisplayName("cache velho com a renovação já feita por outra requisição não grava de novo")
    void doesNotWriteWhenAnotherRequestAlreadyRenewed() {
        var stored = storedSession(NOW.minusSeconds(10), null);
        cacheHit(valid(NOW.minus(Duration.ofMinutes(6))));
        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(stored));

        authenticateSession.execute(secret.getValue());

        verify(sessionRepository, never()).save(any());
    }

    @Test
    @DisplayName("cache ainda diz válida mas o banco já a revogou: session_invalid e nada é gravado")
    void staleCacheOfARevokedSession() {
        cacheHit(valid(NOW.minus(Duration.ofMinutes(6))));
        when(sessionRepository.findById(sessionId))
                .thenReturn(Optional.of(storedSession(NOW.minus(Duration.ofMinutes(6)), NOW.minusSeconds(5))));

        assertThatThrownBy(() -> authenticateSession.execute(secret.getValue()))
                .isInstanceOf(UnauthorizedException.class)
                .satisfies(applicationCode("session_invalid"));

        verify(sessionRepository, never()).save(any());
    }

    @Test
    @DisplayName("renovação devida de sessão que sumiu do banco é session_invalid")
    void missingSessionOnRenewal() {
        cacheHit(valid(NOW.minus(Duration.ofMinutes(6))));
        when(sessionRepository.findById(sessionId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authenticateSession.execute(secret.getValue()))
                .isInstanceOf(UnauthorizedException.class)
                .satisfies(applicationCode("session_invalid"));
    }
}
