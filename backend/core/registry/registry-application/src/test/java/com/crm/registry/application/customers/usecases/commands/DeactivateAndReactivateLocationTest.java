package com.crm.registry.application.customers.usecases.commands;

import com.crm.common.application.exceptions.ForbiddenException;
import com.crm.registry.application.customers.contracts.repositories.LocationRepository;
import com.crm.registry.domain.customers.entities.Customer;
import com.crm.registry.domain.customers.entities.Location;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static com.crm.registry.application.customers.CustomersFixtures.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@DisplayName("DeactivateLocation e ReactivateLocation")
class DeactivateAndReactivateLocationTest {

    private LocationRepository locations;
    private DeactivateLocation deactivate;
    private ReactivateLocation reactivate;

    private final Customer customer = individual();
    private final Location location = location(customer);

    @BeforeEach
    void setUp() {
        locations = mock(LocationRepository.class);
        when(locations.findById(location.getId())).thenReturn(Optional.of(location));
        deactivate = new DeactivateLocation(locations);
        reactivate = new ReactivateLocation(locations);
    }

    @Test
    @DisplayName("desativa e reativa, salvando a cada passo, mesmo com o cliente desativado (ADR-30)")
    void deactivateAndReactivateALocationOfADeactivatedCustomer() {
        customer.deactivate();

        deactivate.execute(new DeactivateLocation.Params(ADMINISTRATOR, location.getId()));
        assertThat(location.isActive()).isFalse();

        reactivate.execute(new ReactivateLocation.Params(COMMERCIAL, location.getId()));
        assertThat(location.isActive()).isTrue();

        verify(locations, times(2)).save(location);
    }

    @Test
    @DisplayName("Técnico é proibido nos dois e o repository não é tocado")
    void technicianIsForbidden() {
        assertThatThrownBy(() -> deactivate.execute(new DeactivateLocation.Params(TECHNICIAN, location.getId())))
                .isInstanceOf(ForbiddenException.class);
        assertThatThrownBy(() -> reactivate.execute(new ReactivateLocation.Params(TECHNICIAN, location.getId())))
                .isInstanceOf(ForbiddenException.class);

        verifyNoInteractions(locations);
    }

    @Test
    @DisplayName("local que não existe é location_not_found nos dois")
    void notFound() {
        var unknown = UUID.randomUUID();
        when(locations.findById(unknown)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> deactivate.execute(new DeactivateLocation.Params(ADMINISTRATOR, unknown)))
                .satisfies(applicationCode("location_not_found"));
        assertThatThrownBy(() -> reactivate.execute(new ReactivateLocation.Params(ADMINISTRATOR, unknown)))
                .satisfies(applicationCode("location_not_found"));

        verify(locations, never()).save(any());
    }

    @Test
    @DisplayName("reativar um local ativo e desativar um desativado falham")
    void invalidTransitionsFail() {
        assertThatThrownBy(() -> reactivate.execute(new ReactivateLocation.Params(ADMINISTRATOR, location.getId())))
                .satisfies(domainCode("location_not_deactivated"));

        deactivate.execute(new DeactivateLocation.Params(ADMINISTRATOR, location.getId()));

        assertThatThrownBy(() -> deactivate.execute(new DeactivateLocation.Params(ADMINISTRATOR, location.getId())))
                .satisfies(domainCode("location_deactivated"));
    }
}
