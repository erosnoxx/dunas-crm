package com.crm.registry.application.customers;

import com.crm.common.application.exceptions.ForbiddenException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.crm.registry.application.customers.CustomersFixtures.*;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("CustomerAccess")
class CustomerAccessTest {

    @Test
    @DisplayName("Administrador e Comercial editam clientes")
    void administratorAndCommercialMayEdit() {
        assertThatCode(() -> CustomerAccess.requireEditor(ADMINISTRATOR)).doesNotThrowAnyException();
        assertThatCode(() -> CustomerAccess.requireEditor(COMMERCIAL)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Técnico recebe forbidden ao editar")
    void technicianIsForbidden() {
        assertThatThrownBy(() -> CustomerAccess.requireEditor(TECHNICIAN))
                .isInstanceOf(ForbiddenException.class)
                .satisfies(applicationCode("forbidden"));
    }

    @Test
    @DisplayName("os três papéis consultam clientes")
    void everyRoleMayRead() {
        assertThatCode(() -> CustomerAccess.requireReader(ADMINISTRATOR)).doesNotThrowAnyException();
        assertThatCode(() -> CustomerAccess.requireReader(COMMERCIAL)).doesNotThrowAnyException();
        assertThatCode(() -> CustomerAccess.requireReader(TECHNICIAN)).doesNotThrowAnyException();
    }
}
