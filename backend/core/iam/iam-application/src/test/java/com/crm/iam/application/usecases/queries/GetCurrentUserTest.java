package com.crm.iam.application.usecases.queries;

import com.crm.common.application.exceptions.ApplicationException;
import com.crm.shared.application.Actor;
import com.crm.iam.application.contracts.readers.UserReader;
import com.crm.iam.application.schemas.CurrentUser;
import com.crm.shared.domain.enums.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("GetCurrentUser")
class GetCurrentUserTest {

    private UserReader userReader;
    private GetCurrentUser getCurrentUser;

    private final Actor actor = new Actor(UUID.randomUUID(), Role.COMMERCIAL, UUID.randomUUID());

    @BeforeEach
    void setUp() {
        userReader = mock(UserReader.class);
        getCurrentUser = new GetCurrentUser(userReader);
    }

    @Test
    @DisplayName("devolve nome, e-mail, papel e se a conta só pode trocar a senha")
    void returnsTheCurrentUser() {
        var current = new CurrentUser(actor.userId(), "Ana Souza", "ana@empresa.com", Role.COMMERCIAL, true);
        when(userReader.findCurrent(actor.userId())).thenReturn(Optional.of(current));

        var result = getCurrentUser.execute(actor);

        assertThat(result).isEqualTo(current);
        assertThat(result.mustChangePassword()).isTrue();
    }

    @Test
    @DisplayName("usuário que não existe mais é user_not_found")
    void unknownUser() {
        when(userReader.findCurrent(actor.userId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> getCurrentUser.execute(actor))
                .isInstanceOfSatisfying(ApplicationException.class,
                        e -> assertThat(e.getCode()).contains("user_not_found"));
    }
}
