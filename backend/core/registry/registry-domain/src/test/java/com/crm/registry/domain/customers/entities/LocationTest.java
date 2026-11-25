package com.crm.registry.domain.customers.entities;

import com.crm.common.domain.exceptions.DomainException;
import com.crm.registry.domain.customers.enums.CustomerStatus;
import com.crm.registry.domain.customers.vos.CustomerName;
import com.crm.registry.domain.customers.vos.LocationName;
import com.crm.shared.domain.enums.UF;
import com.crm.shared.domain.vos.address.*;
import com.crm.shared.domain.vos.contact.Phone;
import com.crm.shared.domain.vos.identity.Document;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Location")
class LocationTest {

    private static final LocationName NAME = LocationName.of("Casa de praia");
    private static final LocationName OTHER_NAME = LocationName.of("Sítio");

    private static Address address(AddressComplement complement) {
        return new Address(
                ZipCode.of("01310-100"),
                Street.of("Avenida Paulista"),
                AddressNumber.of("1578"),
                complement,
                Neighborhood.of("Bela Vista"),
                CityName.of("São Paulo"),
                UF.SP
        );
    }

    private static Customer activeCustomer() {
        return Customer.register(Document.of("529.982.247-25"), CustomerName.of("Maria Souza"),
                null, null, Phone.of("+5511999999999"));
    }

    private static void assertFailsWith(Runnable action, String code, String message) {
        assertThatThrownBy(action::run)
                .isInstanceOf(DomainException.class)
                .satisfies(e -> {
                    assertThat(((DomainException) e).getCode()).contains(code);
                    assertThat(e).hasMessage(message);
                });
    }

    @Test
    @DisplayName("nasce ativo para um cliente ativo e guarda o customerId")
    void registersActiveForAnActiveCustomer() {
        var customer = activeCustomer();

        var location = Location.register(customer, NAME, address(null));

        assertThat(location.getId()).isNotNull();
        assertThat(location.getVersion()).isEmpty();
        assertThat(location.getCustomerId()).isEqualTo(customer.getId());
        assertThat(location.getName()).isEqualTo(NAME);
        assertThat(location.getStatus()).isEqualTo(CustomerStatus.ACTIVE);
        assertThat(location.isActive()).isTrue();
    }

    @Test
    @DisplayName("não nasce para um cliente desativado")
    void registerForADeactivatedCustomerFails() {
        var customer = activeCustomer();
        customer.deactivate();

        assertFailsWith(() -> Location.register(customer, NAME, address(null)),
                "customer_deactivated", "este cliente está desativado");
    }

    @Test
    @DisplayName("edição troca nome e endereço")
    void editChangesNameAndAddress() {
        var location = Location.register(activeCustomer(), NAME, address(null));
        var newAddress = address(AddressComplement.of("Bloco B"));

        location.edit(OTHER_NAME, newAddress);

        assertThat(location.getName()).isEqualTo(OTHER_NAME);
        assertThat(location.getAddress()).isEqualTo(newAddress);
    }

    @Test
    @DisplayName("local desativado não é editado")
    void editOfADeactivatedLocationFails() {
        var location = Location.register(activeCustomer(), NAME, address(null));
        location.deactivate();

        assertFailsWith(() -> location.edit(OTHER_NAME, address(null)),
                "location_deactivated", "este local está desativado");
    }

    @Test
    @DisplayName("desativa e reativa, com os dois erros")
    void deactivateAndReactivate() {
        var location = Location.register(activeCustomer(), NAME, address(null));

        assertFailsWith(location::reactivate, "location_not_deactivated", "este local não está desativado");

        location.deactivate();
        assertThat(location.getStatus()).isEqualTo(CustomerStatus.DEACTIVATED);
        assertThat(location.isActive()).isFalse();
        assertFailsWith(location::deactivate, "location_deactivated", "este local está desativado");

        location.reactivate();
        assertThat(location.isActive()).isTrue();
    }

    @Test
    @DisplayName("o complemento pode faltar")
    void complementMayBeAbsent() {
        var location = Location.register(activeCustomer(), NAME, address(null));

        assertThat(location.getAddress().complement()).isNull();
    }

    @Test
    @DisplayName("reconstituição mantém id e versão")
    void reconstituteKeepsIdAndVersion() {
        var id = UUID.randomUUID();
        var customerId = UUID.randomUUID();

        var location = Location.reconstitute(id, 3L, customerId, NAME, address(null), CustomerStatus.DEACTIVATED);

        assertThat(location.getId()).isEqualTo(id);
        assertThat(location.getVersion()).contains(3L);
        assertThat(location.getCustomerId()).isEqualTo(customerId);
        assertThat(location.getStatus()).isEqualTo(CustomerStatus.DEACTIVATED);
    }
}
