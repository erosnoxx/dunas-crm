package com.crm.iam.application.usecases.commands;

import com.crm.common.application.events.EventPublisher;
import com.crm.common.application.exceptions.BadRequestException;
import com.crm.common.application.exceptions.ConflictException;
import com.crm.common.application.exceptions.ForbiddenException;
import com.crm.shared.application.Actor;
import com.crm.iam.application.contracts.repositories.OneTimeTokenRepository;
import com.crm.iam.application.contracts.repositories.UserRepository;
import com.crm.iam.domain.entities.OneTimeToken;
import com.crm.iam.domain.entities.User;
import com.crm.iam.domain.events.OneTimeTokenIssued;
import com.crm.shared.domain.enums.Role;
import com.crm.iam.domain.enums.TokenPurpose;
import com.crm.iam.domain.enums.UserStatus;
import com.crm.shared.domain.vos.contact.Email;
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
import java.util.Optional;
import java.util.UUID;

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

@DisplayName("InviteUser")
class InviteUserTest {

    private UserRepository userRepository;
    private OneTimeTokenRepository tokenRepository;
    private EventPublisher publisher;
    private InviteUser inviteUser;

    private final Actor admin = new Actor(UUID.randomUUID(), Role.ADMINISTRATOR, UUID.randomUUID());

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        tokenRepository = mock(OneTimeTokenRepository.class);
        publisher = mock(EventPublisher.class);

        when(userRepository.findByEmail(any(Email.class))).thenReturn(Optional.empty());

        inviteUser = new InviteUser(userRepository, tokenRepository, publisher, SETTINGS, CLOCK);
    }

    private InviteUser.Params params(String role) {
        return new InviteUser.Params(admin, "Maria Souza", "Maria@X.com", role);
    }

    @Test
    @DisplayName("quem não é Administrador recebe forbidden e nenhum repository é tocado")
    void nonAdministratorIsForbidden() {
        var commercial = new Actor(UUID.randomUUID(), Role.COMMERCIAL, UUID.randomUUID());

        assertThatThrownBy(() -> inviteUser.execute(new InviteUser.Params(commercial, "Maria", "m@x.com", "TECHNICIAN")))
                .isInstanceOf(ForbiddenException.class)
                .satisfies(applicationCode("forbidden"));

        verifyNoInteractions(userRepository, tokenRepository, publisher);
    }

    @Test
    @DisplayName("e-mail já cadastrado é email_already_registered e nada é salvo")
    void emailAlreadyRegistered() {
        when(userRepository.findByEmail(Email.of("maria@x.com"))).thenReturn(Optional.of(user(UserStatus.ACTIVE)));

        assertThatThrownBy(() -> inviteUser.execute(params("TECHNICIAN")))
                .isInstanceOf(ConflictException.class)
                .satisfies(applicationCode("email_already_registered"));

        verify(userRepository, never()).save(any());
        verifyNoInteractions(tokenRepository, publisher);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "ROOT", "technician"})
    @DisplayName("papel inválido é BadRequestException e nada é tocado")
    void invalidRole(String role) {
        assertThatThrownBy(() -> inviteUser.execute(params(role)))
                .isInstanceOf(BadRequestException.class);

        verifyNoInteractions(userRepository, tokenRepository, publisher);
    }

    @Test
    @DisplayName("sucesso salva o usuário convidado, depois o token de convite de 48 h, depois publica")
    @SuppressWarnings("unchecked")
    void invitesTheUser() {
        var response = inviteUser.execute(params("TECHNICIAN"));

        var savedUser = ArgumentCaptor.forClass(User.class);
        var savedToken = ArgumentCaptor.forClass(OneTimeToken.class);
        var events = ArgumentCaptor.forClass(Collection.class);

        InOrder order = inOrder(userRepository, tokenRepository, publisher);
        order.verify(userRepository).save(savedUser.capture());
        order.verify(tokenRepository).save(savedToken.capture());
        order.verify(publisher, times(2)).publishAll(events.capture());

        var user = savedUser.getValue();
        assertThat(response.id()).isEqualTo(user.getId());
        assertThat(user.getStatus()).isEqualTo(UserStatus.INVITED);
        assertThat(user.getRole()).isEqualTo(Role.TECHNICIAN);
        assertThat(user.getEmail()).isEqualTo(Email.of("maria@x.com"));

        var token = savedToken.getValue();
        assertThat(token.getUserId()).isEqualTo(user.getId());
        assertThat(token.getPurpose()).isEqualTo(TokenPurpose.INVITATION);
        assertThat(token.getExpiresAt()).isEqualTo(NOW.plus(Duration.ofHours(48)));

        assertThat(events.getAllValues().stream().flatMap(Collection::stream))
                .anySatisfy(event -> assertThat(event).isInstanceOfSatisfying(OneTimeTokenIssued.class, issued ->
                        assertThat(issued.userId()).isEqualTo(user.getId())));
    }
}
