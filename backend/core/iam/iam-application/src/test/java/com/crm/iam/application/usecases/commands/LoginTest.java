package com.crm.iam.application.usecases.commands;

import com.crm.common.application.exceptions.RateLimitExceededException;
import com.crm.common.application.exceptions.UnauthorizedException;
import com.crm.common.application.ratelimit.RateLimiter;
import com.crm.iam.application.contracts.repositories.SessionRepository;
import com.crm.iam.application.contracts.repositories.UserRepository;
import com.crm.iam.application.contracts.services.PasswordHasher;
import com.crm.iam.domain.entities.Session;
import com.crm.iam.domain.vos.Password;
import com.crm.iam.domain.vos.TokenSecret;
import com.crm.iam.domain.vos.UserStatus;
import com.crm.shared.domain.vos.contact.Email;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InOrder;

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

@DisplayName("Login")
class LoginTest {

    private UserRepository userRepository;
    private SessionRepository sessionRepository;
    private PasswordHasher hasher;
    private RateLimiter rateLimiter;
    private Login login;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        sessionRepository = mock(SessionRepository.class);
        hasher = mock(PasswordHasher.class);
        rateLimiter = mock(RateLimiter.class);

        when(hasher.hash(any())).thenReturn(DUMMY_HASH);

        login = new Login(userRepository, sessionRepository, hasher, rateLimiter, SETTINGS, CLOCK);
    }

    private Login.Params params(String email) {
        return new Login.Params(email, STRONG_PASSWORD, "Galaxy A54", ORIGIN);
    }

    private void passwordMatches(boolean matches) {
        when(hasher.matches(any(Password.class), eq(USER_HASH))).thenReturn(matches);
    }

    @Test
    @DisplayName("acerto devolve a resposta com o token uma única vez e salva a sessão só com o hash")
    void succeedsAndSavesSession() {
        var user = user(UserStatus.ACTIVE);
        when(userRepository.findByEmail(Email.of("joao@x.com"))).thenReturn(Optional.of(user));
        passwordMatches(true);

        var response = login.execute(params("joao@x.com"));

        var saved = ArgumentCaptor.forClass(Session.class);
        verify(sessionRepository).save(saved.capture());

        assertThat(saved.getValue().getUserId()).isEqualTo(user.getId());
        assertThat(saved.getValue().getTokenHash()).isEqualTo(TokenSecret.of(response.token()).hash());
        assertThat(saved.getValue().getDevice().getValue()).isEqualTo("Galaxy A54");
        assertThat(response.expiresAt()).isEqualTo(NOW.plus(SETTINGS.sessionValidity()));
        assertThat(response.mustChangePassword()).isFalse();
    }

    @Test
    @DisplayName("devolve mustChangePassword da conta")
    void returnsMustChangePassword() {
        when(userRepository.findByEmail(Email.of("joao@x.com"))).thenReturn(Optional.of(userMustChangePassword()));
        passwordMatches(true);

        assertThat(login.execute(params("joao@x.com")).mustChangePassword()).isTrue();
    }

    @Test
    @DisplayName("consome os dois limites antes de conferir a senha e devolve os dois no acerto")
    void consumesBeforeAndRefundsOnSuccess() {
        when(userRepository.findByEmail(Email.of("joao@x.com"))).thenReturn(Optional.of(user(UserStatus.ACTIVE)));
        passwordMatches(true);

        login.execute(params("joao@x.com"));

        InOrder order = inOrder(rateLimiter, hasher, sessionRepository);
        order.verify(rateLimiter).consume(LOGIN_PER_EMAIL, "joao@x.com");
        order.verify(rateLimiter).consume(LOGIN_PER_ORIGIN, ORIGIN);
        order.verify(hasher).matches(any(Password.class), eq(USER_HASH));
        order.verify(rateLimiter).refund(LOGIN_PER_EMAIL, "joao@x.com");
        order.verify(rateLimiter).refund(LOGIN_PER_ORIGIN, ORIGIN);
        order.verify(sessionRepository).save(any(Session.class));
    }

    @Test
    @DisplayName("senha errada é invalid_credentials, não devolve os limites e não abre sessão")
    void wrongPasswordDoesNotRefund() {
        when(userRepository.findByEmail(Email.of("joao@x.com"))).thenReturn(Optional.of(user(UserStatus.ACTIVE)));
        passwordMatches(false);

        assertThatThrownBy(() -> login.execute(params("joao@x.com")))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("e-mail ou senha incorretos")
                .satisfies(applicationCode("invalid_credentials"));

        verify(rateLimiter, never()).refund(any(), any());
        verify(sessionRepository, never()).save(any());
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "fraca", "semespecial1A", "SEMMINUSCULA#1"})
    @DisplayName("senha fora da regra é credencial errada: consome os limites, roda o hasher contra o hash fixo e não devolve nada")
    void passwordOutsideThePolicyIsAFailedCredential(String raw) {
        when(userRepository.findByEmail(Email.of("joao@x.com"))).thenReturn(Optional.of(user(UserStatus.ACTIVE)));
        when(hasher.matches(any(Password.class), any())).thenReturn(true);

        assertThatThrownBy(() -> login.execute(new Login.Params("joao@x.com", raw, "Galaxy A54", ORIGIN)))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("e-mail ou senha incorretos")
                .satisfies(applicationCode("invalid_credentials"));

        InOrder order = inOrder(rateLimiter, hasher);
        order.verify(rateLimiter).consume(LOGIN_PER_EMAIL, "joao@x.com");
        order.verify(rateLimiter).consume(LOGIN_PER_ORIGIN, ORIGIN);
        order.verify(hasher).matches(any(Password.class), eq(DUMMY_HASH));

        verify(hasher, never()).matches(any(Password.class), eq(USER_HASH));
        verify(rateLimiter, never()).refund(any(), any());
        verify(sessionRepository, never()).save(any());
    }

    @Test
    @DisplayName("e-mail inexistente é a mesma invalid_credentials e o hasher roda contra o hash fixo")
    void unknownEmailRunsHasherAgainstDummyHash() {
        when(userRepository.findByEmail(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> login.execute(params("ninguem@x.com")))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("e-mail ou senha incorretos")
                .satisfies(applicationCode("invalid_credentials"));

        verify(hasher).matches(any(Password.class), eq(DUMMY_HASH));
        verify(rateLimiter, never()).refund(any(), any());
    }

    @Test
    @DisplayName("usuário convidado é a mesma invalid_credentials e o hasher roda contra o hash fixo")
    void invitedUserRunsHasherAgainstDummyHash() {
        when(userRepository.findByEmail(any())).thenReturn(Optional.of(user(UserStatus.INVITED)));

        assertThatThrownBy(() -> login.execute(params("joao@x.com")))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("e-mail ou senha incorretos")
                .satisfies(applicationCode("invalid_credentials"));

        verify(hasher).matches(any(Password.class), eq(DUMMY_HASH));
    }

    @Test
    @DisplayName("usuário desativado, mesmo com a senha certa, é a mesma invalid_credentials e usa o hash fixo")
    void deactivatedUserRunsHasherAgainstDummyHash() {
        when(userRepository.findByEmail(any())).thenReturn(Optional.of(user(UserStatus.DEACTIVATED)));
        passwordMatches(true);

        assertThatThrownBy(() -> login.execute(params("joao@x.com")))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("e-mail ou senha incorretos")
                .satisfies(applicationCode("invalid_credentials"));

        verify(hasher).matches(any(Password.class), eq(DUMMY_HASH));
        verify(hasher, never()).matches(any(Password.class), eq(USER_HASH));
        verify(sessionRepository, never()).save(any());
    }

    @Test
    @DisplayName("e-mail em caixa diferente encontra o usuário e conta no limite do e-mail normalizado")
    void emailIsCaseInsensitive() {
        when(userRepository.findByEmail(Email.of("joao@x.com"))).thenReturn(Optional.of(user(UserStatus.ACTIVE)));
        passwordMatches(true);

        login.execute(params("Joao@X.com"));

        verify(userRepository).findByEmail(Email.of("joao@x.com"));
        verify(rateLimiter).consume(LOGIN_PER_EMAIL, "joao@x.com");
    }

    @Test
    @DisplayName("limite estourado propaga rate_limited e não consulta o repository")
    void rateLimitExceededStopsBeforeLookup() {
        doThrow(new RateLimitExceededException("rate_limited", LIMITED))
                .when(rateLimiter).consume(eq(LOGIN_PER_EMAIL), any());

        assertThatThrownBy(() -> login.execute(params("joao@x.com")))
                .isInstanceOf(RateLimitExceededException.class)
                .satisfies(applicationCode("rate_limited"));

        verifyNoInteractions(userRepository);
        verifyNoInteractions(sessionRepository);
    }

    @Test
    @DisplayName("limite por origem estourado também para antes da consulta")
    void originLimitExceededStopsBeforeLookup() {
        doThrow(new RateLimitExceededException("rate_limited", LIMITED))
                .when(rateLimiter).consume(eq(LOGIN_PER_ORIGIN), any());

        assertThatThrownBy(() -> login.execute(params("joao@x.com")))
                .isInstanceOf(RateLimitExceededException.class);

        verifyNoInteractions(userRepository);
    }
}
