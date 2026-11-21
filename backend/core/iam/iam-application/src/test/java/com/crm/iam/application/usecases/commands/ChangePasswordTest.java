package com.crm.iam.application.usecases.commands;

import com.crm.common.application.cache.Cache;
import com.crm.common.application.events.EventPublisher;
import com.crm.common.application.exceptions.RateLimitExceededException;
import com.crm.common.application.exceptions.UnprocessableEntityException;
import com.crm.common.application.ratelimit.RateLimiter;
import com.crm.shared.application.Actor;
import com.crm.iam.application.contracts.repositories.SessionRepository;
import com.crm.iam.application.contracts.repositories.UserRepository;
import com.crm.iam.application.contracts.services.PasswordHasher;
import com.crm.iam.domain.entities.Session;
import com.crm.iam.domain.entities.User;
import com.crm.iam.domain.vos.Password;
import com.crm.shared.domain.enums.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InOrder;

import java.util.List;
import java.util.Optional;

import static com.crm.iam.application.usecases.commands.IamFixtures.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@DisplayName("ChangePassword")
class ChangePasswordTest {

    private static final String CURRENT_PASSWORD = "Atual#Senha1";

    private UserRepository userRepository;
    private SessionRepository sessionRepository;
    private PasswordHasher hasher;
    private RateLimiter rateLimiter;
    private EventPublisher publisher;
    private Cache cache;
    private ChangePassword changePassword;

    private final User user = userMustChangePassword();
    private final Session current = session(user.getId());
    private final Actor actor = new Actor(user.getId(), Role.COMMERCIAL, current.getId());

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        sessionRepository = mock(SessionRepository.class);
        hasher = mock(PasswordHasher.class);
        rateLimiter = mock(RateLimiter.class);
        publisher = mock(EventPublisher.class);
        cache = mock(Cache.class);

        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(hasher.hash(any(Password.class))).thenReturn(NEW_HASH);

        changePassword = new ChangePassword(
                userRepository,
                sessionRepository,
                hasher,
                rateLimiter,
                publisher,
                cache,
                SETTINGS,
                CLOCK
        );
    }

    private ChangePassword.Params params() {
        return new ChangePassword.Params(actor, CURRENT_PASSWORD, STRONG_PASSWORD);
    }

    private void currentPasswordMatches(boolean matches) {
        when(hasher.matches(any(Password.class), eq(USER_HASH))).thenReturn(matches);
    }

    @Test
    @DisplayName("senha atual errada é wrong_current_password, conta no limite do e-mail e não troca nada")
    void wrongCurrentPassword() {
        currentPasswordMatches(false);

        assertThatThrownBy(() -> changePassword.execute(params()))
                .isInstanceOf(UnprocessableEntityException.class)
                .hasMessage("a senha atual está incorreta")
                .satisfies(applicationCode("wrong_current_password"));

        verify(rateLimiter).consume(LOGIN_PER_EMAIL, "joao@x.com");
        verify(rateLimiter, never()).refund(any(), any());
        verify(userRepository, never()).save(any());
        verifyNoInteractions(sessionRepository, publisher);
    }

    @Test
    @DisplayName("sucesso troca o hash, zera mustChangePassword, devolve o limite e salva o usuário")
    void changesPassword() {
        currentPasswordMatches(true);
        when(sessionRepository.findActiveByUserId(user.getId(), NOW)).thenReturn(List.of(current));

        changePassword.execute(params());

        InOrder order = inOrder(rateLimiter, hasher, userRepository);
        order.verify(rateLimiter).consume(LOGIN_PER_EMAIL, "joao@x.com");
        order.verify(hasher).matches(any(Password.class), eq(USER_HASH));
        order.verify(rateLimiter).refund(LOGIN_PER_EMAIL, "joao@x.com");
        order.verify(hasher).hash(any(Password.class));
        order.verify(userRepository).save(user);

        assertThat(user.getPasswordHash()).contains(NEW_HASH);
        assertThat(user.isMustChangePassword()).isFalse();
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "fraca", "semespecial1A"})
    @DisplayName("senha atual fora da regra é wrong_current_password, conta no limite e não troca nada")
    void currentPasswordOutsideThePolicyIsWrong(String raw) {
        assertThatThrownBy(() -> changePassword.execute(new ChangePassword.Params(actor, raw, STRONG_PASSWORD)))
                .isInstanceOf(UnprocessableEntityException.class)
                .hasMessage("a senha atual está incorreta")
                .satisfies(applicationCode("wrong_current_password"));

        verify(rateLimiter).consume(LOGIN_PER_EMAIL, "joao@x.com");
        verify(rateLimiter, never()).refund(any(), any());
        verify(userRepository, never()).save(any());
        verifyNoInteractions(sessionRepository, publisher, cache);
    }

    @Test
    @DisplayName("toString dos parâmetros não traz a senha atual nem a nova")
    void paramsToStringDoesNotLeakSecrets() {
        var text = params().toString();

        assertThat(text).doesNotContain(CURRENT_PASSWORD).doesNotContain(STRONG_PASSWORD);
    }

    @Test
    @DisplayName("senha nova fora da regra é invalid_password sem gastar tentativa do limite de login")
    void invalidNewPasswordDoesNotConsumeTheLimit() {
        assertThatThrownBy(() -> changePassword.execute(new ChangePassword.Params(actor, CURRENT_PASSWORD, WEAK_PASSWORD)))
                .satisfies(domainCode("invalid_password"));

        verifyNoInteractions(rateLimiter, hasher, sessionRepository, publisher, cache);
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("tira do cache a sessão atual, com a chave iam:session:<hash>, logo depois de salvar o usuário")
    void evictsTheCurrentSessionFromTheCache() {
        currentPasswordMatches(true);
        var other = session(user.getId());
        when(sessionRepository.findActiveByUserId(user.getId(), NOW)).thenReturn(List.of(other, current));

        changePassword.execute(params());

        InOrder order = inOrder(userRepository, cache);
        order.verify(userRepository).save(user);
        order.verify(cache).evict("iam:session:" + current.getTokenHash().getValue());
        verify(cache, never()).evict("iam:session:" + other.getTokenHash().getValue());
    }

    @Test
    @DisplayName("revoga as outras sessões do usuário e mantém a do Actor")
    void revokesOtherSessionsAndKeepsCurrent() {
        currentPasswordMatches(true);
        var other = session(user.getId());
        when(sessionRepository.findActiveByUserId(user.getId(), NOW)).thenReturn(List.of(current, other));

        changePassword.execute(params());

        assertThat(other.getRevokedAt()).contains(NOW);
        assertThat(other.getRevokedBy()).contains(user.getId());
        assertThat(current.getRevokedAt()).isEmpty();

        InOrder order = inOrder(sessionRepository, publisher);
        order.verify(sessionRepository).save(other);
        order.verify(publisher).publishAll(any());
        verify(sessionRepository, never()).save(current);
    }

    @Test
    @DisplayName("senha nova fraca é invalid_password e nada é trocado")
    void weakNewPassword() {
        currentPasswordMatches(true);

        assertThatThrownBy(() -> changePassword.execute(new ChangePassword.Params(actor, CURRENT_PASSWORD, WEAK_PASSWORD)))
                .satisfies(domainCode("invalid_password"));

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("limite estourado propaga rate_limited e não confere a senha")
    void rateLimitExceeded() {
        doThrow(new RateLimitExceededException("rate_limited", LIMITED))
                .when(rateLimiter).consume(eq(LOGIN_PER_EMAIL), any());

        assertThatThrownBy(() -> changePassword.execute(params()))
                .isInstanceOf(RateLimitExceededException.class)
                .satisfies(applicationCode("rate_limited"));

        verifyNoInteractions(hasher);
    }

    @Test
    @DisplayName("usuário que não existe é user_not_found")
    void unknownUser() {
        when(userRepository.findById(user.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> changePassword.execute(params()))
                .satisfies(applicationCode("user_not_found"));

    }
}
