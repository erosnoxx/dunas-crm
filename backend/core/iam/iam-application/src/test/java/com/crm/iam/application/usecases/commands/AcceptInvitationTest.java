package com.crm.iam.application.usecases.commands;

import com.crm.common.application.exceptions.UnprocessableEntityException;
import com.crm.common.domain.exceptions.ValidationException;
import com.crm.iam.application.contracts.repositories.OneTimeTokenRepository;
import com.crm.iam.application.contracts.repositories.SessionRepository;
import com.crm.iam.application.contracts.repositories.UserRepository;
import com.crm.iam.application.contracts.services.PasswordHasher;
import com.crm.iam.domain.entities.OneTimeToken;
import com.crm.iam.domain.entities.Session;
import com.crm.iam.domain.entities.User;
import com.crm.iam.domain.vos.Password;
import com.crm.iam.domain.vos.TokenPurpose;
import com.crm.iam.domain.vos.TokenSecret;
import com.crm.iam.domain.vos.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

import java.time.Duration;
import java.util.Optional;

import static com.crm.iam.application.usecases.commands.IamFixtures.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@DisplayName("AcceptInvitation")
class AcceptInvitationTest {

    private OneTimeTokenRepository tokenRepository;
    private UserRepository userRepository;
    private SessionRepository sessionRepository;
    private PasswordHasher hasher;
    private AcceptInvitation acceptInvitation;

    private final TokenSecret secret = TokenSecret.generate();
    private final User invited = user(UserStatus.INVITED);

    @BeforeEach
    void setUp() {
        tokenRepository = mock(OneTimeTokenRepository.class);
        userRepository = mock(UserRepository.class);
        sessionRepository = mock(SessionRepository.class);
        hasher = mock(PasswordHasher.class);

        when(hasher.hash(any(Password.class))).thenReturn(NEW_HASH);
        when(userRepository.findById(invited.getId())).thenReturn(Optional.of(invited));

        acceptInvitation = new AcceptInvitation(
                tokenRepository,
                userRepository,
                sessionRepository,
                hasher,
                SETTINGS,
                CLOCK
        );
    }

    private AcceptInvitation.Params params(String password) {
        return new AcceptInvitation.Params(secret.getValue(), password, "Galaxy A54");
    }

    private OneTimeToken pendingToken(TokenPurpose purpose) {
        var token = OneTimeToken.issue(invited.getId(), purpose, secret, Duration.ofHours(48), NOW.minusSeconds(60));

        when(tokenRepository.findByTokenHash(secret.hash())).thenReturn(Optional.of(token));
        when(tokenRepository.consume(token)).thenReturn(true);

        return token;
    }

    @Test
    @DisplayName("sucesso consome o link, ativa o usuário com o novo hash e abre a sessão, nessa ordem")
    void activatesUserAndOpensSession() {
        var token = pendingToken(TokenPurpose.INVITATION);

        var response = acceptInvitation.execute(params(STRONG_PASSWORD));

        InOrder order = inOrder(tokenRepository, hasher, userRepository, sessionRepository);
        order.verify(tokenRepository).consume(token);
        order.verify(hasher).hash(any(Password.class));
        order.verify(userRepository).save(invited);
        order.verify(sessionRepository).save(any(Session.class));

        assertThat(invited.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(invited.getPasswordHash()).contains(NEW_HASH);
        assertThat(token.getUsedAt()).contains(NOW);
        assertThat(response.expiresAt()).isEqualTo(NOW.plus(SETTINGS.sessionValidity()));
        assertThat(response.mustChangePassword()).isFalse();

        var saved = ArgumentCaptor.forClass(Session.class);
        verify(sessionRepository).save(saved.capture());
        assertThat(saved.getValue().getUserId()).isEqualTo(invited.getId());
        assertThat(saved.getValue().getTokenHash()).isEqualTo(TokenSecret.of(response.token()).hash());
    }

    @Test
    @DisplayName("nunca salva o token depois do consume")
    void neverSavesTheToken() {
        pendingToken(TokenPurpose.INVITATION);

        acceptInvitation.execute(params(STRONG_PASSWORD));

        verify(tokenRepository, never()).save(any());
    }

    @Test
    @DisplayName("token desconhecido é link_used, sem distinguir de usado")
    void unknownTokenIsLinkUsed() {
        when(tokenRepository.findByTokenHash(secret.hash())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> acceptInvitation.execute(params(STRONG_PASSWORD)))
                .isInstanceOf(UnprocessableEntityException.class)
                .satisfies(applicationCode("link_used"));

        verify(tokenRepository, never()).consume(any());
    }

    @Test
    @DisplayName("link de redefinição não aceita convite: é link_used")
    void otherPurposeIsLinkUsed() {
        pendingToken(TokenPurpose.PASSWORD_RESET);

        assertThatThrownBy(() -> acceptInvitation.execute(params(STRONG_PASSWORD)))
                .isInstanceOf(UnprocessableEntityException.class)
                .satisfies(applicationCode("link_used"));

        verify(tokenRepository, never()).consume(any());
    }

    @Test
    @DisplayName("link expirado é link_expired e não chega ao update condicional")
    void expiredTokenIsLinkExpired() {
        var expired = OneTimeToken.issue(
                invited.getId(),
                TokenPurpose.INVITATION,
                secret,
                Duration.ofHours(48),
                NOW.minus(Duration.ofHours(49))
        );
        when(tokenRepository.findByTokenHash(secret.hash())).thenReturn(Optional.of(expired));

        assertThatThrownBy(() -> acceptInvitation.execute(params(STRONG_PASSWORD)))
                .satisfies(domainCode("link_expired"));

        verify(tokenRepository, never()).consume(any());
        verifyNoInteractions(userRepository, sessionRepository);
    }

    @Test
    @DisplayName("consume devolvendo false (outra requisição venceu) é link_used")
    void lostRaceIsLinkUsed() {
        var token = pendingToken(TokenPurpose.INVITATION);
        when(tokenRepository.consume(token)).thenReturn(false);

        assertThatThrownBy(() -> acceptInvitation.execute(params(STRONG_PASSWORD)))
                .isInstanceOf(UnprocessableEntityException.class)
                .satisfies(applicationCode("link_used"));

        verify(userRepository, never()).save(any());
        verify(sessionRepository, never()).save(any());
    }

    @Test
    @DisplayName("senha fraca é invalid_password e o link não é consumido")
    void weakPasswordDoesNotBurnTheLink() {
        pendingToken(TokenPurpose.INVITATION);

        assertThatThrownBy(() -> acceptInvitation.execute(params(WEAK_PASSWORD)))
                .isInstanceOf(ValidationException.class)
                .satisfies(domainCode("invalid_password"));

        verify(tokenRepository, never()).consume(any());
        verifyNoInteractions(userRepository, sessionRepository);
    }
}
