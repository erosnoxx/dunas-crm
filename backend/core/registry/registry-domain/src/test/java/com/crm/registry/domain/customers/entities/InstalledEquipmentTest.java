package com.crm.registry.domain.customers.entities;

import com.crm.common.domain.exceptions.DomainException;
import com.crm.registry.domain.catalog.entities.EquipmentModel;
import com.crm.registry.domain.catalog.enums.CatalogStatus;
import com.crm.registry.domain.catalog.vos.ModelName;
import com.crm.registry.domain.customers.enums.CustomerStatus;
import com.crm.registry.domain.customers.vos.CustomerName;
import com.crm.registry.domain.customers.vos.InstallationDate;
import com.crm.registry.domain.customers.vos.LocationName;
import com.crm.registry.domain.customers.vos.SerialNumber;
import com.crm.shared.domain.enums.UF;
import com.crm.shared.domain.vos.address.*;
import com.crm.shared.domain.vos.contact.Phone;
import com.crm.shared.domain.vos.identity.Document;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("InstalledEquipment")
class InstalledEquipmentTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-06-15T12:00:00Z"), ZoneOffset.UTC);
    private static final SerialNumber SERIAL = SerialNumber.of("SN-0001");
    private static final SerialNumber OTHER_SERIAL = SerialNumber.of("SN-0002");
    private static final InstallationDate DATE = InstallationDate.of(LocalDate.of(2026, 1, 10), CLOCK);
    private static final InstallationDate OTHER_DATE = InstallationDate.of(LocalDate.of(2026, 3, 20), CLOCK);

    private static Customer activeCustomer() {
        return Customer.register(Document.of("529.982.247-25"), CustomerName.of("Maria Souza"),
                null, null, Phone.of("+5511999999999"));
    }

    private static Location locationOf(Customer customer) {
        var address = new Address(
                ZipCode.of("01310-100"),
                Street.of("Avenida Paulista"),
                AddressNumber.of("1578"),
                null,
                Neighborhood.of("Bela Vista"),
                CityName.of("São Paulo"),
                UF.SP
        );
        return Location.register(customer, LocationName.of("Casa de praia"), address);
    }

    private static EquipmentModel model(CatalogStatus status) {
        return EquipmentModel.reconstitute(UUID.randomUUID(), 0L, UUID.randomUUID(), UUID.randomUUID(),
                ModelName.of("SUN2000-5K"), List.of(), status);
    }

    private static InstalledEquipment installed(Customer customer, Location location) {
        return InstalledEquipment.install(location, customer, model(CatalogStatus.ACTIVE), SERIAL, DATE);
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
    @DisplayName("nasce ativo com o local e o modelo")
    void installsActiveWithModelAndLocation() {
        var customer = activeCustomer();
        var location = locationOf(customer);
        var model = model(CatalogStatus.ACTIVE);

        var equipment = InstalledEquipment.install(location, customer, model, SERIAL, DATE);

        assertThat(equipment.getId()).isNotNull();
        assertThat(equipment.getVersion()).isEmpty();
        assertThat(equipment.getLocationId()).isEqualTo(location.getId());
        assertThat(equipment.getModelId()).isEqualTo(model.getId());
        assertThat(equipment.getSerialNumber()).isEqualTo(SERIAL);
        assertThat(equipment.getInstalledOn()).contains(DATE);
        assertThat(equipment.getStatus()).isEqualTo(CustomerStatus.ACTIVE);
        assertThat(equipment.isActive()).isTrue();
    }

    @Test
    @DisplayName("a data de instalação pode faltar")
    void installWithoutDateKeepsItAbsent() {
        var customer = activeCustomer();

        var equipment = InstalledEquipment.install(locationOf(customer), customer, model(CatalogStatus.ACTIVE), SERIAL, null);

        assertThat(equipment.getInstalledOn()).isEmpty();
    }

    @Test
    @DisplayName("não nasce em um local desativado")
    void installInADeactivatedLocationFails() {
        var customer = activeCustomer();
        var location = locationOf(customer);
        location.deactivate();

        assertFailsWith(() -> InstalledEquipment.install(location, customer, model(CatalogStatus.ACTIVE), SERIAL, DATE),
                "location_deactivated", "este local está desativado");
    }

    @Test
    @DisplayName("não nasce para um cliente desativado, mesmo com o local ativo (sem propagação)")
    void installForADeactivatedCustomerWithAnActiveLocationFails() {
        var customer = activeCustomer();
        var location = locationOf(customer);
        customer.deactivate();

        assertThat(location.isActive()).isTrue();
        assertFailsWith(() -> InstalledEquipment.install(location, customer, model(CatalogStatus.ACTIVE), SERIAL, DATE),
                "customer_deactivated", "este cliente está desativado");
    }

    @Test
    @DisplayName("não nasce com um modelo desativado")
    void installOfADeactivatedModelFails() {
        var customer = activeCustomer();
        var location = locationOf(customer);

        assertFailsWith(() -> InstalledEquipment.install(location, customer, model(CatalogStatus.DEACTIVATED), SERIAL, DATE),
                "model_deactivated", "este modelo está desativado");
    }

    @Test
    @DisplayName("aceita um modelo cujo fabricante está desativado: só o modelo é conferido")
    void installAcceptsAModelWhoseManufacturerIsDeactivated() {
        var customer = activeCustomer();

        var equipment = installed(customer, locationOf(customer));

        assertThat(equipment.isActive()).isTrue();
    }

    @Test
    @DisplayName("o local de outro cliente falha sem código")
    void installWithAnotherCustomersLocationFailsWithoutCode() {
        var owner = activeCustomer();
        var location = locationOf(owner);
        var other = activeCustomer();

        assertThatThrownBy(() -> InstalledEquipment.install(location, other, model(CatalogStatus.ACTIVE), SERIAL, DATE))
                .isInstanceOf(DomainException.class)
                .satisfies(e -> {
                    assertThat(((DomainException) e).getCode()).isEmpty();
                    assertThat(e).hasMessage("o cliente informado não é o dono do local");
                });
    }

    @Test
    @DisplayName("edição troca série e data, nunca o local nem o modelo")
    void editChangesSerialAndDateButNeverLocationNorModel() {
        var customer = activeCustomer();
        var location = locationOf(customer);
        var model = model(CatalogStatus.ACTIVE);
        var equipment = InstalledEquipment.install(location, customer, model, SERIAL, DATE);

        equipment.edit(OTHER_SERIAL, OTHER_DATE);

        assertThat(equipment.getSerialNumber()).isEqualTo(OTHER_SERIAL);
        assertThat(equipment.getInstalledOn()).contains(OTHER_DATE);
        assertThat(equipment.getLocationId()).isEqualTo(location.getId());
        assertThat(equipment.getModelId()).isEqualTo(model.getId());

        equipment.edit(OTHER_SERIAL, null);

        assertThat(equipment.getInstalledOn()).isEmpty();
    }

    @Test
    @DisplayName("equipamento desativado não é editado")
    void editOfADeactivatedEquipmentFails() {
        var customer = activeCustomer();
        var equipment = installed(customer, locationOf(customer));
        equipment.deactivate();

        assertFailsWith(() -> equipment.edit(OTHER_SERIAL, OTHER_DATE),
                "installed_equipment_deactivated", "este equipamento está desativado");
    }

    @Test
    @DisplayName("desativa e reativa, com os dois erros")
    void deactivateAndReactivate() {
        var customer = activeCustomer();
        var equipment = installed(customer, locationOf(customer));

        assertFailsWith(equipment::reactivate,
                "installed_equipment_not_deactivated", "este equipamento não está desativado");

        equipment.deactivate();
        assertThat(equipment.getStatus()).isEqualTo(CustomerStatus.DEACTIVATED);
        assertThat(equipment.isActive()).isFalse();
        assertFailsWith(equipment::deactivate,
                "installed_equipment_deactivated", "este equipamento está desativado");

        equipment.reactivate();
        assertThat(equipment.isActive()).isTrue();
    }

    @Test
    @DisplayName("reconstituição mantém id e versão")
    void reconstituteKeepsIdAndVersion() {
        var id = UUID.randomUUID();

        var equipment = InstalledEquipment.reconstitute(id, 3L, UUID.randomUUID(), UUID.randomUUID(),
                SERIAL, null, CustomerStatus.DEACTIVATED);

        assertThat(equipment.getId()).isEqualTo(id);
        assertThat(equipment.getVersion()).contains(3L);
        assertThat(equipment.getInstalledOn()).isEmpty();
        assertThat(equipment.getStatus()).isEqualTo(CustomerStatus.DEACTIVATED);
    }
}
