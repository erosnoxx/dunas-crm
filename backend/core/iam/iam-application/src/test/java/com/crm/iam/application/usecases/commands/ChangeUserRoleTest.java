package com.crm.iam.application.usecases.commands;

import com.crm.common.application.events.EventPublisher;
import com.crm.common.application.exceptions.BadRequestException;
import com.crm.common.application.exceptions.ForbiddenException;
import com.crm.shared.application.Actor;
import com.crm.iam.application.contracts.repositories.UserRepository;
import com.crm.iam.domain.entities.User;
import com.crm.iam.domain.events.UserRoleChanged;
import com.crm.shared.domain.enums.Role;
import com.crm.iam.domain.vos.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@DisplayName("ChangeUserRole")
class ChangeUserRoleTest {

    private UserRepository userRepository;
    private EventPublisher publisher;
    private ChangeUserRole changeUserRole;

    private final Actor admin = new Actor(UUID.randomUUID(), Role.ADMINISTRATOR, UUID.randomUUID());
    private final User target = user(UserStatus.ACTIVE);

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        publisher = mock(EventPublisher.class);

        when(userRepository.findById(target.getId())).thenReturn(Optional.of(target));

        changeUserRole = new ChangeUserRole(userRepository, publisher, CLOCK);
    }

    @Test
    @DisplayName("quem não é Administrador recebe forbidden e nenhum repository é tocado")
    void nonAdministratorIsForbidden() {
        var commercial = new Actor(UUID.randomUUID(), Role.COMMERCIAL, UUID.randomUUID());

        assertThatThrownBy(() -> changeUserRole.execute(new ChangeUserRole.Params(commercial, target.getId(), "TECHNICIAN")))
                .isInstanceOf(ForbiddenException.class)
                .satisfies(applicationCode("forbidden"));

        verifyNoInteractions(userRepository, publisher);
    }

    @Test
    @DisplayName("usuário que não existe é user_not_found")
    void unknownUser() {
        var unknown = UUID.randomUUID();
        when(userRepository.findById(unknown)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> changeUserRole.execute(new ChangeUserRole.Params(admin, unknown, "TECHNICIAN")))
                .satisfies(applicationCode("user_not_found"));
    }

    @Test
    @DisplayName("papel inválido é BadRequestException e nada é tocado")
    void invalidRole() {
        assertThatThrownBy(() -> changeUserRole.execute(new ChangeUserRole.Params(admin, target.getId(), "ROOT")))
                .isInstanceOf(BadRequestException.class);

        verifyNoInteractions(userRepository, publisher);
    }

    @Test
    @DisplayName("mudar o próprio papel deixa passar cannot_change_own_role do domínio")
    void ownRole() {
        var self = new Actor(target.getId(), Role.ADMINISTRATOR, UUID.randomUUID());

        assertThatThrownBy(() -> changeUserRole.execute(new ChangeUserRole.Params(self, target.getId(), "TECHNICIAN")))
                .satisfies(domainCode("cannot_change_own_role"));

        verify(userRepository, never()).save(any());
        verifyNoInteractions(publisher);
    }

    @Test
    @DisplayName("sucesso troca o papel, salva e publica UserRoleChanged")
    @SuppressWarnings("unchecked")
    void changesTheRole() {
        changeUserRole.execute(new ChangeUserRole.Params(admin, target.getId(), "TECHNICIAN"));

        assertThat(target.getRole()).isEqualTo(Role.TECHNICIAN);

        InOrder order = inOrder(userRepository, publisher);
        order.verify(userRepository).save(target);
        var events = ArgumentCaptor.forClass(Collection.class);
        order.verify(publisher).publishAll(events.capture());

        assertThat((Collection<Object>) events.getValue())
                .singleElement()
                .isInstanceOf(UserRoleChanged.class);
    }
}
