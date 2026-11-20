package com.crm.iam.application.usecases.commands;

import com.crm.common.application.events.EventPublisher;
import com.crm.common.application.exceptions.RateLimitExceededException;
import com.crm.common.application.ratelimit.RateLimiter;
import com.crm.common.domain.events.DomainEvent;
import com.crm.iam.application.contracts.repositories.OneTimeTokenRepository;
import com.crm.iam.application.contracts.repositories.UserRepository;
import com.crm.iam.domain.entities.OneTimeToken;
import com.crm.iam.domain.events.OneTimeTokenIssued;
import com.crm.iam.domain.vos.TokenPurpose;
import com.crm.iam.domain.vos.UserStatus;
import com.crm.shared.domain.vos.contact.Email;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

import java.util.Collection;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@DisplayName("RequestPasswordReset")
class RequestPasswordResetTest {

    private UserRepository userRepository;
    private OneTimeTokenRepository tokenRepository;
    private RateLimiter rateLimiter;
    private EventPublisher publisher;
    private RequestPasswordReset requestPasswordReset;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        tokenRepository = mock(OneTimeTokenRepository.class);
        rateLimiter = mock(RateLimiter.class);
        publisher = mock(EventPublisher.class);

        requestPasswordReset = new RequestPasswordReset(
                userRepository,
                tokenRepository,
                rateLimiter,
                publisher,
                SETTINGS,
                CLOCK
        );
    }

    private RequestPasswordReset.Params params(String email) {
        return new RequestPasswordReset.Params(email, ORIGIN);
    }

    @Test
    @DisplayName("e-mail desconhecido retorna normalmente e não emite token")
    void unknownEmailDoesNothing() {
        when(userRepository.findByEmail(any())).thenReturn(Optional.empty());

        requestPasswordReset.execute(params("ninguem@x.com"));

        verifyNoInteractions(tokenRepository, publisher);
    }

    @Test
    @DisplayName("usuário convidado retorna normalmente e não emite token")
    void invitedUserDoesNothing() {
        when(userRepository.findByEmail(any())).thenReturn(Optional.of(user(UserStatus.INVITED)));

        requestPasswordReset.execute(params("joao@x.com"));

        verifyNoInteractions(tokenRepository, publisher);
    }

    @Test
    @DisplayName("usuário desativado retorna normalmente e não emite token")
    void deactivatedUserDoesNothing() {
        when(userRepository.findByEmail(any())).thenReturn(Optional.of(user(UserStatus.DEACTIVATED)));

        requestPasswordReset.execute(params("joao@x.com"));

        verifyNoInteractions(tokenRepository, publisher);
    }

    @Test
    @DisplayName("usuário ativo tem os pendentes invalidados, recebe um token novo e o OneTimeTokenIssued é publicado")
    void activeUserGetsNewToken() {
        var user = user(UserStatus.ACTIVE);
        var pending = token(user.getId(), TokenPurpose.PASSWORD_RESET, NOW.minusSeconds(600));
        pending.pullEvents();
        when(userRepository.findByEmail(Email.of("joao@x.com"))).thenReturn(Optional.of(user));
        when(tokenRepository.findPending(user.getId(), TokenPurpose.PASSWORD_RESET)).thenReturn(List.of(pending));

        requestPasswordReset.execute(params("Joao@X.com"));

        assertThat(pending.getInvalidatedAt()).contains(NOW);

        var issued = ArgumentCaptor.forClass(OneTimeToken.class);
        InOrder order = inOrder(rateLimiter, tokenRepository, publisher);
        order.verify(rateLimiter).consume(RESET_PER_EMAIL, "joao@x.com");
        order.verify(tokenRepository).save(pending);
        order.verify(tokenRepository).save(issued.capture());
        order.verify(publisher).publishAll(any());

        var token = issued.getValue();
        assertThat(token.getUserId()).isEqualTo(user.getId());
        assertThat(token.getPurpose()).isEqualTo(TokenPurpose.PASSWORD_RESET);
        assertThat(token.getExpiresAt()).isEqualTo(NOW.plus(SETTINGS.passwordResetValidity()));
    }

    @Test
    @DisplayName("publica os eventos puxados do token emitido, com o segredo só no evento")
    @SuppressWarnings("unchecked")
    void publishesIssuedEvent() {
        var user = user(UserStatus.ACTIVE);
        when(userRepository.findByEmail(any())).thenReturn(Optional.of(user));
        when(tokenRepository.findPending(any(), eq(TokenPurpose.PASSWORD_RESET))).thenReturn(List.of());

        requestPasswordReset.execute(params("joao@x.com"));

        var events = ArgumentCaptor.forClass(Collection.class);
        verify(publisher).publishAll(events.capture());

        var published = (Collection<DomainEvent>) events.getValue();
        assertThat(published).singleElement().isInstanceOfSatisfying(OneTimeTokenIssued.class, event -> {
            assertThat(event.userId()).isEqualTo(user.getId());
            assertThat(event.purpose()).isEqualTo(TokenPurpose.PASSWORD_RESET);
            assertThat(event.secret()).isNotNull();
        });
    }

    @Test
    @DisplayName("limite estourado propaga rate_limited e não consulta o repository")
    void rateLimitExceeded() {
        doThrow(new RateLimitExceededException("rate_limited", LIMITED))
                .when(rateLimiter).consume(eq(RESET_PER_EMAIL), any());

        assertThatThrownBy(() -> requestPasswordReset.execute(params("joao@x.com")))
                .isInstanceOf(RateLimitExceededException.class)
                .satisfies(applicationCode("rate_limited"));

        verifyNoInteractions(userRepository, tokenRepository, publisher);
    }
}
