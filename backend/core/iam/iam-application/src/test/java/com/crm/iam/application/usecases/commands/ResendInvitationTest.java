package com.crm.iam.application.usecases.commands;

import com.crm.common.application.events.EventPublisher;
import com.crm.common.application.exceptions.ForbiddenException;
import com.crm.iam.application.Actor;
import com.crm.iam.application.contracts.repositories.OneTimeTokenRepository;
import com.crm.iam.application.contracts.repositories.UserRepository;
import com.crm.iam.domain.entities.OneTimeToken;
import com.crm.iam.domain.entities.User;
import com.crm.iam.domain.events.OneTimeTokenIssued;
import com.crm.iam.domain.vos.Role;
import com.crm.iam.domain.vos.TokenPurpose;
import com.crm.iam.domain.vos.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

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

@DisplayName("ResendInvitation")
class ResendInvitationTest {

    private UserRepository userRepository;
    private OneTimeTokenRepository tokenRepository;
    private EventPublisher publisher;
    private ResendInvitation resendInvitation;

    private final Actor admin = new Actor(UUID.randomUUID(), Role.ADMINISTRATOR, UUID.randomUUID());
    private final User invited = user(UserStatus.INVITED);

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        tokenRepository = mock(OneTimeTokenRepository.class);
        publisher = mock(EventPublisher.class);

        when(userRepository.findById(invited.getId())).thenReturn(Optional.of(invited));

        resendInvitation = new ResendInvitation(userRepository, tokenRepository, publisher, SETTINGS, CLOCK);
    }

    @Test
    @DisplayName("quem não é Administrador recebe forbidden e nenhum repository é tocado")
    void nonAdministratorIsForbidden() {
        var technician = new Actor(UUID.randomUUID(), Role.TECHNICIAN, UUID.randomUUID());

        assertThatThrownBy(() -> resendInvitation.execute(new ResendInvitation.Params(technician, invited.getId())))
                .isInstanceOf(ForbiddenException.class)
                .satisfies(applicationCode("forbidden"));

        verifyNoInteractions(userRepository, tokenRepository, publisher);
    }

    @Test
    @DisplayName("usuário que não existe é user_not_found")
    void unknownUser() {
        var unknown = UUID.randomUUID();
        when(userRepository.findById(unknown)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> resendInvitation.execute(new ResendInvitation.Params(admin, unknown)))
                .satisfies(applicationCode("user_not_found"));

        verifyNoInteractions(tokenRepository, publisher);
    }

    @ParameterizedTest
    @EnumSource(value = UserStatus.class, names = "INVITED", mode = EnumSource.Mode.EXCLUDE)
    @DisplayName("quem não está INVITED é user_not_invited e nada é emitido")
    void notInvited(UserStatus status) {
        var other = user(status);
        when(userRepository.findById(other.getId())).thenReturn(Optional.of(other));

        assertThatThrownBy(() -> resendInvitation.execute(new ResendInvitation.Params(admin, other.getId())))
                .satisfies(applicationCode("user_not_invited"));

        verifyNoInteractions(tokenRepository, publisher);
    }

    @Test
    @DisplayName("invalida os convites pendentes antes de emitir o novo e publica OneTimeTokenIssued")
    @SuppressWarnings("unchecked")
    void invalidatesPendingThenIssues() {
        var pending = token(invited.getId(), TokenPurpose.INVITATION, NOW.minusSeconds(3600));
        when(tokenRepository.findPending(invited.getId(), TokenPurpose.INVITATION)).thenReturn(List.of(pending));

        resendInvitation.execute(new ResendInvitation.Params(admin, invited.getId()));

        assertThat(pending.getInvalidatedAt()).contains(NOW);

        var issued = ArgumentCaptor.forClass(OneTimeToken.class);
        var events = ArgumentCaptor.forClass(Collection.class);
        InOrder order = inOrder(tokenRepository, publisher);
        order.verify(tokenRepository).save(pending);
        order.verify(tokenRepository).save(issued.capture());
        order.verify(publisher).publishAll(events.capture());

        assertThat(issued.getValue().getPurpose()).isEqualTo(TokenPurpose.INVITATION);
        assertThat(issued.getValue().getExpiresAt()).isEqualTo(NOW.plus(Duration.ofHours(48)));
        assertThat((Collection<Object>) events.getValue())
                .singleElement()
                .isInstanceOf(OneTimeTokenIssued.class);
        verify(userRepository, never()).save(any());
    }
}
