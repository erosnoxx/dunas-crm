package com.crm.iam.application.usecases.commands;

import com.crm.common.application.exceptions.ForbiddenException;
import com.crm.shared.application.Actor;
import com.crm.iam.application.contracts.repositories.UserRepository;
import com.crm.shared.domain.enums.Role;
import com.crm.iam.domain.enums.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.Optional;
import java.util.UUID;

import static com.crm.iam.application.usecases.commands.IamFixtures.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@DisplayName("ReactivateUser")
class ReactivateUserTest {

    private UserRepository userRepository;
    private ReactivateUser reactivateUser;

    private final Actor admin = new Actor(UUID.randomUUID(), Role.ADMINISTRATOR, UUID.randomUUID());

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);

        reactivateUser = new ReactivateUser(userRepository);
    }

    @Test
    @DisplayName("quem não é Administrador recebe forbidden e nenhum repository é tocado")
    void nonAdministratorIsForbidden() {
        var commercial = new Actor(UUID.randomUUID(), Role.COMMERCIAL, UUID.randomUUID());

        assertThatThrownBy(() -> reactivateUser.execute(new ReactivateUser.Params(commercial, UUID.randomUUID())))
                .isInstanceOf(ForbiddenException.class)
                .satisfies(applicationCode("forbidden"));

        verifyNoInteractions(userRepository);
    }

    @Test
    @DisplayName("usuário que não existe é user_not_found")
    void unknownUser() {
        var unknown = UUID.randomUUID();
        when(userRepository.findById(unknown)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reactivateUser.execute(new ReactivateUser.Params(admin, unknown)))
                .satisfies(applicationCode("user_not_found"));
    }

    @ParameterizedTest
    @EnumSource(value = UserStatus.class, names = "DEACTIVATED", mode = EnumSource.Mode.EXCLUDE)
    @DisplayName("fora de DEACTIVATED deixa passar user_not_deactivated do domínio")
    void notDeactivated(UserStatus status) {
        var target = user(status);
        when(userRepository.findById(target.getId())).thenReturn(Optional.of(target));

        assertThatThrownBy(() -> reactivateUser.execute(new ReactivateUser.Params(admin, target.getId())))
                .satisfies(domainCode("user_not_deactivated"));

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("sucesso reativa e salva")
    void reactivates() {
        var target = user(UserStatus.DEACTIVATED);
        when(userRepository.findById(target.getId())).thenReturn(Optional.of(target));

        reactivateUser.execute(new ReactivateUser.Params(admin, target.getId()));

        assertThat(target.getStatus()).isEqualTo(UserStatus.ACTIVE);
        verify(userRepository).save(target);
    }
}
