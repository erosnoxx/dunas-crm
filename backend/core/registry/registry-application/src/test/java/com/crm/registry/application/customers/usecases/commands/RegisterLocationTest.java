package com.crm.registry.application.customers.usecases.commands;

import com.crm.common.application.exceptions.BadRequestException;
import com.crm.common.application.exceptions.ForbiddenException;
import com.crm.common.application.exceptions.InvalidParam;
import com.crm.common.domain.exceptions.ValidationException;
import com.crm.registry.application.customers.contracts.repositories.CustomerRepository;
import com.crm.registry.application.customers.contracts.repositories.LocationRepository;
import com.crm.registry.application.customers.schemas.AddressInput;
import com.crm.registry.domain.customers.entities.Customer;
import com.crm.registry.domain.customers.entities.Location;
import com.crm.shared.domain.enums.UF;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

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

@DisplayName("RegisterLocation")
class RegisterLocationTest {

    private CustomerRepository customers;
    private LocationRepository locations;
    private RegisterLocation registerLocation;

    private final Customer customer = individual();

    @BeforeEach
    void setUp() {
        customers = mock(CustomerRepository.class);
        locations = mock(LocationRepository.class);
        when(customers.findById(customer.getId())).thenReturn(Optional.of(customer));
        registerLocation = new RegisterLocation(customers, locations);
    }

    private RegisterLocation.Params params(UUID customerId, AddressInput address) {
        return new RegisterLocation.Params(ADMINISTRATOR, customerId, "Casa de praia", address);
    }

    private Location saved(LocationRepository repository) {
        var captor = ArgumentCaptor.forClass(Location.class);
        verify(repository).save(captor.capture());
        return captor.getValue();
    }

    @Test
    @DisplayName("cadastra o local de um cliente ativo e devolve o id")
    void registersForAnActiveCustomer() {
        var response = registerLocation.execute(params(customer.getId(), addressInput()));

        var location = saved(locations);
        assertThat(response.id()).isEqualTo(location.getId());
        assertThat(location.getCustomerId()).isEqualTo(customer.getId());
        assertThat(location.isActive()).isTrue();
        assertThat(location.getAddress().state()).isEqualTo(UF.SP);
    }

    @Test
    @DisplayName("Técnico é proibido e nenhum repository é tocado")
    void technicianIsForbiddenAndTheRepositoriesAreUntouched() {
        assertThatThrownBy(() -> registerLocation.execute(
                new RegisterLocation.Params(TECHNICIAN, customer.getId(), " ", null)))
                .isInstanceOf(ForbiddenException.class);

        verifyNoInteractions(customers, locations);
    }

    @Test
    @DisplayName("cep inválido de cliente inexistente é erro de formato e o cliente nunca é lido")
    void invalidZipCodeOfAnUnknownCustomerIsABadRequest() {
        var address = new AddressInput("123", "Rua", "1", null, "Centro", "Recife", "PE");

        assertThatThrownBy(() -> registerLocation.execute(params(UUID.randomUUID(), address)))
                .isInstanceOf(ValidationException.class);

        verifyNoInteractions(customers, locations);
    }

    @Test
    @DisplayName("cep com e sem traço guarda os mesmos dígitos")
    void zipCodeWithAndWithoutDashStoresTheSameDigits() {
        registerLocation.execute(params(customer.getId(), addressInput()));
        var withDash = saved(locations).getAddress().zipCode();

        var other = mock(LocationRepository.class);
        var plain = new AddressInput("01310100", "Avenida Paulista", "1578", null, "Bela Vista", "São Paulo", "SP");
        new RegisterLocation(customers, other).execute(params(customer.getId(), plain));

        assertThat(saved(other).getAddress().zipCode()).isEqualTo(withDash);
    }

    @Test
    @DisplayName("cep com 7 dígitos é erro de formato")
    void zipCodeWith7DigitsIsABadRequest() {
        var address = new AddressInput("0131010", "Rua", "1", null, "Centro", "Recife", "PE");

        assertThatThrownBy(() -> registerLocation.execute(params(customer.getId(), address)))
                .isInstanceOf(ValidationException.class);

        verify(locations, never()).save(any());
    }

    @Test
    @DisplayName("uf desconhecida é 400 e nomeia o parâmetro address.state")
    void unknownStateIsABadRequestNamingTheParam() {
        var address = new AddressInput("01310-100", "Rua", "1", null, "Centro", "Recife", "XX");

        assertThatThrownBy(() -> registerLocation.execute(params(UUID.randomUUID(), address)))
                .isInstanceOfSatisfying(BadRequestException.class, e ->
                        assertThat(e.getInvalidParams())
                                .containsExactly(new InvalidParam("address.state", "uf desconhecida")));

        verifyNoInteractions(customers, locations);
    }

    @Test
    @DisplayName("complemento nulo é ausente")
    void nullComplementIsAbsent() {
        registerLocation.execute(params(customer.getId(), addressInput()));

        assertThat(saved(locations).getAddress().complement()).isNull();
    }

    @Test
    @DisplayName("cliente que não existe é customer_not_found")
    void unknownCustomerIsNotFound() {
        assertThatThrownBy(() -> registerLocation.execute(params(UUID.randomUUID(), addressInput())))
                .satisfies(applicationCode("customer_not_found"));

        verify(locations, never()).save(any());
    }

    @Test
    @DisplayName("cliente desativado é customer_deactivated e nada é salvo")
    void registerForADeactivatedCustomerFails() {
        customer.deactivate();

        assertThatThrownBy(() -> registerLocation.execute(params(customer.getId(), addressInput())))
                .satisfies(domainCode("customer_deactivated"));

        verify(locations, never()).save(any());
    }
}
