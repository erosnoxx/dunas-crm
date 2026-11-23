package com.crm.registry.application.customers.usecases.commands;

import com.crm.common.application.exceptions.ForbiddenException;
import com.crm.common.domain.exceptions.ValidationException;
import com.crm.registry.application.customers.contracts.repositories.CustomerRepository;
import com.crm.registry.application.customers.contracts.repositories.LocationRepository;
import com.crm.registry.domain.customers.vos.LocationName;
import com.crm.shared.domain.enums.UF;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@DisplayName("UpdateLocation")
class UpdateLocationTest {

    private LocationRepository locations;
    private UpdateLocation updateLocation;

    private final Customer customer = individual();
    private final Location location = location(customer);

    @BeforeEach
    void setUp() {
        locations = mock(LocationRepository.class);
        when(locations.findById(location.getId())).thenReturn(Optional.of(location));
        updateLocation = new UpdateLocation(locations);
    }

    private UpdateLocation.Params params(UUID id) {
        return new UpdateLocation.Params(COMMERCIAL, id, "Sítio", addressInput());
    }

    @Test
    @DisplayName("edita nome e endereço e salva")
    void updates() {
        updateLocation.execute(params(location.getId()));

        assertThat(location.getName()).isEqualTo(LocationName.of("Sítio"));
        assertThat(location.getAddress().state()).isEqualTo(UF.SP);
        verify(locations).save(location);
    }

    @Test
    @DisplayName("Técnico é proibido e o repository não é tocado")
    void technicianIsForbidden() {
        assertThatThrownBy(() -> updateLocation.execute(
                new UpdateLocation.Params(TECHNICIAN, location.getId(), " ", null)))
                .isInstanceOf(ForbiddenException.class);

        verifyNoInteractions(locations);
    }

    @Test
    @DisplayName("nome inválido de local inexistente é erro de formato e findById nunca é chamado")
    void invalidNameOfAnUnknownLocationIsABadRequest() {
        assertThatThrownBy(() -> updateLocation.execute(
                new UpdateLocation.Params(ADMINISTRATOR, UUID.randomUUID(), " ", addressInput())))
                .isInstanceOf(ValidationException.class);

        verify(locations, never()).findById(any());
    }

    @Test
    @DisplayName("local que não existe é location_not_found")
    void notFound() {
        var unknown = UUID.randomUUID();
        when(locations.findById(unknown)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> updateLocation.execute(params(unknown)))
                .satisfies(applicationCode("location_not_found"));

        verify(locations, never()).save(any());
    }

    @Test
    @DisplayName("local desativado é location_deactivated")
    void updateOfADeactivatedLocationFails() {
        location.deactivate();

        assertThatThrownBy(() -> updateLocation.execute(params(location.getId())))
                .satisfies(domainCode("location_deactivated"));

        verify(locations, never()).save(any());
    }

    @Test
    @DisplayName("local de cliente desativado salva e o cliente não é lido (ADR-30)")
    void updateOfALocationOfADeactivatedCustomerSaves() {
        var customers = mock(CustomerRepository.class);
        customer.deactivate();

        updateLocation.execute(params(location.getId()));

        verify(locations).save(location);
        verifyNoInteractions(customers);
    }
}
