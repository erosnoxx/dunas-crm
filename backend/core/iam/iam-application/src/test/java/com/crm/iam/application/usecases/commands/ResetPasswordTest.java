package com.crm.iam.application.usecases.commands;

import com.crm.common.application.events.EventPublisher;
import com.crm.common.application.exceptions.UnprocessableEntityException;
import com.crm.common.domain.events.DomainEvent;
import com.crm.common.domain.exceptions.ValidationException;
import com.crm.iam.application.contracts.repositories.OneTimeTokenRepository;
import com.crm.iam.application.contracts.repositories.SessionRepository;
import com.crm.iam.application.contracts.repositories.UserRepository;
import com.crm.iam.application.contracts.services.PasswordHasher;
import com.crm.iam.domain.entities.OneTimeToken;
import com.crm.iam.domain.entities.User;
import com.crm.iam.domain.events.SessionRevoked;
import com.crm.iam.domain.vos.Password;
import com.crm.iam.domain.vos.TokenPurpose;
import com.crm.iam.domain.vos.TokenSecret;
import com.crm.iam.domain.vos.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static com.crm.iam.application.usecases.commands.IamFixtures.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@DisplayName("ResetPassword")
class ResetPasswordTest {

    private OneTimeTokenRepository tokenRepository;
    private UserRepository userRepository;
    private SessionRepository sessionRepository;
    private PasswordHasher hasher;
    private EventPublisher publisher;
    private ResetPassword resetPassword;

    private final TokenSecret secret = TokenSecret.generate();
    private final User active = user(UserStatus.ACTIVE);

    @BeforeEach
    void setUp() {
        tokenRepository = mock(OneTimeTokenRepository.class);
        userRepository = mock(UserRepository.class);
        sessionRepository = mock(SessionRepository.class);
        hasher = mock(PasswordHasher.class);
        publisher = mock(EventPublisher.class);

        when(hasher.hash(any(Password.class))).thenReturn(NEW_HASH);
        when(userRepository.findById(active.getId())).thenReturn(Optional.of(active));

        resetPassword = new ResetPassword(
                tokenRepository,
                userRepository,
                sessionRepository,
                hasher,
                publisher,
                CLOCK
        );
    }

    private ResetPassword.Params params(String password) {
        return new ResetPassword.Params(secret.getValue(), password);
    }

    private OneTimeToken pendingToken() {
        var token = OneTimeToken.issue(
                active.getId(),
                TokenPurpose.PASSWORD_RESET,
                secret,
                Duration.ofHours(1),
                NOW.minusSeconds(60)
        );

        when(tokenRepository.findByTokenHash(secret.hash())).thenReturn(Optional.of(token));
        when(tokenRepository.consume(token)).thenReturn(true);

        return token;
    }

    @Test
    @DisplayName("consome o link, troca o hash e revoga todas as sessões ativas publicando SessionRevoked de cada uma")
    @SuppressWarnings("unchecked")
    void resetsAndRevokesAllSessions() {
        var token = pendingToken();
        var first = session(active.getId());
        var second = session(active.getId());
        when(sessionRepository.findActiveByUserId(active.getId(), NOW)).thenReturn(List.of(first, second));

        resetPassword.execute(params(STRONG_PASSWORD));

        InOrder order = inOrder(tokenRepository, hasher, userRepository, sessionRepository, publisher);
        order.verify(tokenRepository).consume(token);
        order.verify(hasher).hash(any(Password.class));
        order.verify(userRepository).save(active);
        order.verify(sessionRepository).save(first);
        order.verify(publisher).publishAll(any());
        order.verify(sessionRepository).save(second);
        order.verify(publisher).publishAll(any());

        assertThat(active.getPasswordHash()).contains(NEW_HASH);
        assertThat(first.getRevokedAt()).contains(NOW);
        assertThat(second.getRevokedAt()).contains(NOW);

        var events = ArgumentCaptor.forClass(Collection.class);
        verify(publisher, times(2)).publishAll(events.capture());

        var published = events.getAllValues().stream()
                .flatMap(batch -> ((Collection<DomainEvent>) batch).stream())
                .toList();
        assertThat(published).hasSize(2).allSatisfy(event -> assertThat(event).isInstanceOf(SessionRevoked.class));
    }

    @Test
    @DisplayName("toString dos parâmetros não traz o segredo do link nem a senha")
    void paramsToStringDoesNotLeakSecrets() {
        var text = params(STRONG_PASSWORD).toString();

        assertThat(text).doesNotContain(secret.getValue()).doesNotContain(STRONG_PASSWORD);
    }

    @Test
    @DisplayName("não abre sessão e nunca salva o token depois do consume")
    void doesNotOpenSessionNorSaveToken() {
        pendingToken();
        when(sessionRepository.findActiveByUserId(any(), any())).thenReturn(List.of());

        resetPassword.execute(params(STRONG_PASSWORD));

        verify(sessionRepository, never()).save(any());
        verify(tokenRepository, never()).save(any());
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "curto", "+++++++++++++++++++++++++++++++++++++++++++"})
    @DisplayName("token malformado é link_used, como o desconhecido, e nem consulta o repository")
    void malformedTokenIsLinkUsed(String token) {
        assertThatThrownBy(() -> resetPassword.execute(new ResetPassword.Params(token, STRONG_PASSWORD)))
                .isInstanceOf(UnprocessableEntityException.class)
                .hasMessage("este link já foi usado ou foi substituído")
                .satisfies(applicationCode("link_used"));

        verifyNoInteractions(tokenRepository, sessionRepository, publisher);
    }

    @Test
    @DisplayName("token desconhecido é link_used")
    void unknownTokenIsLinkUsed() {
        when(tokenRepository.findByTokenHash(secret.hash())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> resetPassword.execute(params(STRONG_PASSWORD)))
                .isInstanceOf(UnprocessableEntityException.class)
                .satisfies(applicationCode("link_used"));
    }

    @Test
    @DisplayName("link de convite não redefine senha: é link_used")
    void invitationTokenIsLinkUsed() {
        var token = OneTimeToken.issue(active.getId(), TokenPurpose.INVITATION, secret, Duration.ofHours(1), NOW);
        when(tokenRepository.findByTokenHash(secret.hash())).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> resetPassword.execute(params(STRONG_PASSWORD)))
                .isInstanceOf(UnprocessableEntityException.class)
                .satisfies(applicationCode("link_used"));

        verify(tokenRepository, never()).consume(any());
    }

    @Test
    @DisplayName("consume devolvendo false é link_used e nada é alterado")
    void lostRaceIsLinkUsed() {
        var token = pendingToken();
        when(tokenRepository.consume(token)).thenReturn(false);

        assertThatThrownBy(() -> resetPassword.execute(params(STRONG_PASSWORD)))
                .isInstanceOf(UnprocessableEntityException.class)
                .satisfies(applicationCode("link_used"));

        verify(userRepository, never()).save(any());
        verifyNoInteractions(sessionRepository, publisher);
    }

    @Test
    @DisplayName("link expirado é link_expired")
    void expiredTokenIsLinkExpired() {
        var token = OneTimeToken.issue(
                active.getId(),
                TokenPurpose.PASSWORD_RESET,
                secret,
                Duration.ofHours(1),
                NOW.minus(Duration.ofHours(2))
        );
        when(tokenRepository.findByTokenHash(secret.hash())).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> resetPassword.execute(params(STRONG_PASSWORD)))
                .satisfies(domainCode("link_expired"));

        verify(tokenRepository, never()).consume(any());
    }

    @Test
    @DisplayName("senha fraca é invalid_password e o link não é consumido")
    void weakPasswordDoesNotBurnTheLink() {
        pendingToken();

        assertThatThrownBy(() -> resetPassword.execute(params(WEAK_PASSWORD)))
                .isInstanceOf(ValidationException.class)
                .satisfies(domainCode("invalid_password"));

        verify(tokenRepository, never()).consume(any());
        verifyNoInteractions(userRepository, sessionRepository);
    }

    @Test
    @DisplayName("usuário que não está ativo é user_not_active, vindo do domínio")
    void inactiveUserIsRejectedByTheDomain() {
        var deactivated = user(UserStatus.DEACTIVATED);
        var token = OneTimeToken.issue(
                deactivated.getId(),
                TokenPurpose.PASSWORD_RESET,
                secret,
                Duration.ofHours(1),
                NOW
        );
        when(tokenRepository.findByTokenHash(secret.hash())).thenReturn(Optional.of(token));
        when(tokenRepository.consume(token)).thenReturn(true);
        when(userRepository.findById(deactivated.getId())).thenReturn(Optional.of(deactivated));

        assertThatThrownBy(() -> resetPassword.execute(params(STRONG_PASSWORD)))
                .satisfies(domainCode("user_not_active"));

        verify(userRepository, never()).save(any());
    }
}
