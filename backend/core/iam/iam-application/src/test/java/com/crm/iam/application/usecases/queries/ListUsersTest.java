package com.crm.iam.application.usecases.queries;

import com.crm.common.application.exceptions.ForbiddenException;
import com.crm.common.application.pages.Page;
import com.crm.common.application.pages.Pageable;
import com.crm.shared.application.Actor;
import com.crm.iam.application.contracts.readers.UserReader;
import com.crm.iam.application.schemas.UserFilter;
import com.crm.iam.application.schemas.UserSummary;
import com.crm.shared.domain.enums.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@DisplayName("ListUsers")
class ListUsersTest {

    private UserReader userReader;
    private ListUsers listUsers;

    private final UserFilter filter = new UserFilter(null, null, "ana");
    private final Pageable pageable = Pageable.of(0, 10, "name", "ASC");

    @BeforeEach
    void setUp() {
        userReader = mock(UserReader.class);
        listUsers = new ListUsers(userReader);
    }

    @Test
    @DisplayName("o Administrador recebe a página que o leitor devolve")
    void administratorGetsThePage() {
        var administrator = new Actor(UUID.randomUUID(), Role.ADMINISTRATOR, UUID.randomUUID());
        var page = new Page<UserSummary>(List.of(), 0, 10, 0, 0);
        when(userReader.search(filter, pageable)).thenReturn(page);

        assertThat(listUsers.execute(new ListUsers.Params(administrator, filter, pageable))).isSameAs(page);
    }

    @Test
    @DisplayName("quem não é Administrador recebe forbidden e o leitor nem é consultado")
    void nonAdministratorIsForbidden() {
        var technician = new Actor(UUID.randomUUID(), Role.TECHNICIAN, UUID.randomUUID());

        assertThatThrownBy(() -> listUsers.execute(new ListUsers.Params(technician, filter, pageable)))
                .isInstanceOf(ForbiddenException.class);

        verifyNoInteractions(userReader);
    }
}
