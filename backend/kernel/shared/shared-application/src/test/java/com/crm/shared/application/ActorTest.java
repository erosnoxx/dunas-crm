package com.crm.shared.application;

import com.crm.common.application.exceptions.ForbiddenException;
import com.crm.shared.domain.enums.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Actor")
class ActorTest {

    private static Actor actorOf(Role role) {
        return new Actor(UUID.randomUUID(), role, UUID.randomUUID());
    }

    @Test
    @DisplayName("requireAnyRole deixa passar um papel da lista")
    void requireAnyRoleAllowsAListedRole() {
        assertThatCode(() -> actorOf(Role.COMMERCIAL).requireAnyRole(Role.ADMINISTRATOR, Role.COMMERCIAL))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("requireAnyRole recusa um papel fora da lista")
    void requireAnyRoleRefusesAnotherRole() {
        assertThatThrownBy(() -> actorOf(Role.TECHNICIAN).requireAnyRole(Role.ADMINISTRATOR, Role.COMMERCIAL))
                .isInstanceOfSatisfying(ForbiddenException.class,
                        e -> assertThat(e.getCode()).contains("forbidden"));
    }

    @Test
    @DisplayName("requireAdministrator recusa o Comercial")
    void requireAdministratorRefusesCommercial() {
        assertThatThrownBy(() -> actorOf(Role.COMMERCIAL).requireAdministrator())
                .isInstanceOf(ForbiddenException.class);
    }
}
