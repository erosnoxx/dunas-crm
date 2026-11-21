package com.crm.registry.application.catalog;

import com.crm.common.application.exceptions.ForbiddenException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.crm.registry.application.catalog.CatalogFixtures.*;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("CatalogAccess")
class CatalogAccessTest {

    @Test
    @DisplayName("Administrador e Comercial editam o catálogo")
    void administratorAndCommercialMayEdit() {
        assertThatCode(() -> CatalogAccess.requireEditor(ADMINISTRATOR)).doesNotThrowAnyException();
        assertThatCode(() -> CatalogAccess.requireEditor(COMMERCIAL)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Técnico recebe forbidden")
    void technicianIsForbidden() {
        assertThatThrownBy(() -> CatalogAccess.requireEditor(TECHNICIAN))
                .isInstanceOf(ForbiddenException.class)
                .satisfies(applicationCode("forbidden"));
    }

    @Test
    @DisplayName("os três papéis consultam, mas o ator nulo é erro de programação")
    void everyRoleMayReadButANullActorIsAProgrammingError() {
        assertThatCode(() -> CatalogAccess.requireReader(ADMINISTRATOR)).doesNotThrowAnyException();
        assertThatCode(() -> CatalogAccess.requireReader(COMMERCIAL)).doesNotThrowAnyException();
        assertThatCode(() -> CatalogAccess.requireReader(TECHNICIAN)).doesNotThrowAnyException();

        assertThatThrownBy(() -> CatalogAccess.requireReader(null)).isInstanceOf(NullPointerException.class);
    }
}
