package com.crm.registry.application.customers.usecases.commands;

import com.crm.common.application.exceptions.ConflictException;
import com.crm.common.application.exceptions.ForbiddenException;
import com.crm.common.domain.exceptions.ValidationException;
import com.crm.registry.application.catalog.contracts.repositories.EquipmentModelRepository;
import com.crm.registry.application.customers.contracts.repositories.CustomerRepository;
import com.crm.registry.application.customers.contracts.repositories.InstalledEquipmentRepository;
import com.crm.registry.application.customers.contracts.repositories.LocationRepository;
import com.crm.registry.domain.catalog.entities.EquipmentModel;
import com.crm.registry.domain.catalog.enums.CatalogStatus;
import com.crm.registry.domain.catalog.vos.ModelName;
import com.crm.registry.domain.customers.entities.Customer;
import com.crm.registry.domain.customers.entities.InstalledEquipment;
import com.crm.registry.domain.customers.entities.Location;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.crm.registry.application.customers.CustomersFixtures.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@DisplayName("InstallEquipment")
class InstallEquipmentTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-06-15T12:00:00Z"), ZoneOffset.UTC);
    private static final LocalDate TODAY = LocalDate.of(2026, 6, 15);

    private InstalledEquipmentRepository repository;
    private LocationRepository locations;
    private CustomerRepository customers;
    private EquipmentModelRepository models;
    private InstallEquipment installEquipment;

    private final Customer customer = individual();
    private final Location location = location(customer);
    private EquipmentModel model = model(CatalogStatus.ACTIVE);

    private static EquipmentModel model(CatalogStatus status) {
        return EquipmentModel.reconstitute(UUID.randomUUID(), 0L, UUID.randomUUID(), UUID.randomUUID(),
                ModelName.of("SUN2000-5K"), List.of(), status);
    }

    @BeforeEach
    void setUp() {
        repository = mock(InstalledEquipmentRepository.class);
        locations = mock(LocationRepository.class);
        customers = mock(CustomerRepository.class);
        models = mock(EquipmentModelRepository.class);
        when(locations.findById(location.getId())).thenReturn(Optional.of(location));
        when(customers.findById(customer.getId())).thenReturn(Optional.of(customer));
        when(models.findById(model.getId())).thenReturn(Optional.of(model));
        installEquipment = new InstallEquipment(repository, locations, customers, models, CLOCK);
    }

    private InstallEquipment.Params params(String serial, LocalDate date) {
        return new InstallEquipment.Params(ADMINISTRATOR, location.getId(), model.getId(), serial, date);
    }

    @Test
    @DisplayName("instala o equipamento e devolve o id")
    void installsAndReturnsTheId() {
        var response = installEquipment.execute(params("SN-1", LocalDate.of(2026, 1, 10)));

        var captor = ArgumentCaptor.forClass(InstalledEquipment.class);
        verify(repository).save(captor.capture());
        assertThat(response.id()).isEqualTo(captor.getValue().getId());
        assertThat(captor.getValue().getLocationId()).isEqualTo(location.getId());
        assertThat(captor.getValue().getModelId()).isEqualTo(model.getId());
        verify(repository).existsByModelAndSerialNumber(any(), any(), isNull());
    }

    @Test
    @DisplayName("Técnico é proibido e nenhum repository é tocado")
    void technicianIsForbidden() {
        assertThatThrownBy(() -> installEquipment.execute(
                new InstallEquipment.Params(TECHNICIAN, location.getId(), model.getId(), " ", null)))
                .isInstanceOf(ForbiddenException.class);

        verifyNoInteractions(repository, locations, customers, models);
    }

    @Test
    @DisplayName("data futura de local inexistente é erro de formato e nenhum repository é lido")
    void futureDateOfAnUnknownLocationIsABadRequest() {
        var unknown = new InstallEquipment.Params(
                ADMINISTRATOR, UUID.randomUUID(), UUID.randomUUID(), "SN-1", TODAY.plusDays(1));

        assertThatThrownBy(() -> installEquipment.execute(unknown))
                .isInstanceOf(ValidationException.class)
                .satisfies(domainCode("invalid_installation_date"));

        verifyNoInteractions(repository, locations, customers, models);
    }

    @Test
    @DisplayName("a data de hoje é aceita")
    void todayIsAccepted() {
        installEquipment.execute(params("SN-1", TODAY));

        verify(repository).save(any());
    }

    @Test
    @DisplayName("local que não existe é location_not_found")
    void unknownLocationIsNotFound() {
        assertThatThrownBy(() -> installEquipment.execute(
                new InstallEquipment.Params(ADMINISTRATOR, UUID.randomUUID(), model.getId(), "SN-1", null)))
                .satisfies(applicationCode("location_not_found"));

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("modelo que não existe é model_not_found")
    void unknownModelIsModelNotFound() {
        assertThatThrownBy(() -> installEquipment.execute(
                new InstallEquipment.Params(ADMINISTRATOR, location.getId(), UUID.randomUUID(), "SN-1", null)))
                .satisfies(applicationCode("model_not_found"));

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("local desativado é location_deactivated")
    void deactivatedLocationFails() {
        location.deactivate();

        assertThatThrownBy(() -> installEquipment.execute(params("SN-1", null)))
                .satisfies(domainCode("location_deactivated"));

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("local ativo de cliente desativado é customer_deactivated")
    void activeLocationOfADeactivatedCustomerFails() {
        customer.deactivate();

        assertThatThrownBy(() -> installEquipment.execute(params("SN-1", null)))
                .satisfies(domainCode("customer_deactivated"));

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("modelo desativado é model_deactivated e nada é salvo")
    void deactivatedModelFails() {
        model = model(CatalogStatus.DEACTIVATED);
        when(models.findById(model.getId())).thenReturn(Optional.of(model));

        assertThatThrownBy(() -> installEquipment.execute(params("SN-1", null)))
                .satisfies(domainCode("model_deactivated"));

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("série repetida só é conferida depois das regras de ativo")
    void repeatedSerialIsCheckedAfterTheActiveRules() {
        model = model(CatalogStatus.DEACTIVATED);
        when(models.findById(model.getId())).thenReturn(Optional.of(model));
        when(repository.existsByModelAndSerialNumber(any(), any(), any())).thenReturn(true);

        assertThatThrownBy(() -> installEquipment.execute(params("SN-1", null)))
                .satisfies(domainCode("model_deactivated"));

        verify(repository, never()).existsByModelAndSerialNumber(any(), any(), any());
    }

    @Test
    @DisplayName("série repetida no mesmo modelo é conflito")
    void repeatedSerialIsAConflict() {
        when(repository.existsByModelAndSerialNumber(any(), any(), any())).thenReturn(true);

        assertThatThrownBy(() -> installEquipment.execute(params("SN-1", null)))
                .isInstanceOf(ConflictException.class)
                .satisfies(applicationCode("serial_number_taken"))
                .hasMessage("este modelo já tem um equipamento com esse número de série");

        verify(repository, never()).save(any());
    }
}
