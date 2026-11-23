package com.crm.registry.application.customers.usecases.commands;

import com.crm.common.application.exceptions.ForbiddenException;
import com.crm.registry.application.customers.contracts.repositories.InstalledEquipmentRepository;
import com.crm.registry.domain.customers.entities.InstalledEquipment;
import com.crm.registry.domain.customers.enums.CustomerStatus;
import com.crm.registry.domain.customers.vos.InstallationDate;
import com.crm.registry.domain.customers.vos.SerialNumber;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
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

@DisplayName("UpdateInstalledEquipment")
class UpdateInstalledEquipmentTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-06-15T12:00:00Z"), ZoneOffset.UTC);

    private InstalledEquipmentRepository repository;
    private UpdateInstalledEquipment update;

    private InstalledEquipment equipment = equipment(CustomerStatus.ACTIVE);

    private static InstalledEquipment equipment(CustomerStatus status) {
        return InstalledEquipment.reconstitute(UUID.randomUUID(), 0L, UUID.randomUUID(), UUID.randomUUID(),
                SerialNumber.of("SN-1"), InstallationDate.of(LocalDate.of(2026, 1, 10), CLOCK), status);
    }

    @BeforeEach
    void setUp() {
        repository = mock(InstalledEquipmentRepository.class);
        when(repository.findById(equipment.getId())).thenReturn(Optional.of(equipment));
        update = new UpdateInstalledEquipment(repository, CLOCK);
    }

    private UpdateInstalledEquipment.Params params(String serial, LocalDate date) {
        return new UpdateInstalledEquipment.Params(COMMERCIAL, equipment.getId(), serial, date);
    }

    @Test
    @DisplayName("edita série e data e salva")
    void updatesAndSaves() {
        update.execute(params("SN-2", null));

        assertThat(equipment.getSerialNumber()).isEqualTo(SerialNumber.of("SN-2"));
        assertThat(equipment.getInstalledOn()).isEmpty();
        verify(repository).save(equipment);
    }

    @Test
    @DisplayName("Técnico é proibido e o repository não é tocado")
    void technicianIsForbidden() {
        assertThatThrownBy(() -> update.execute(
                new UpdateInstalledEquipment.Params(TECHNICIAN, equipment.getId(), "SN-2", null)))
                .isInstanceOf(ForbiddenException.class);

        verifyNoInteractions(repository);
    }

    @Test
    @DisplayName("data futura de equipamento inexistente é erro de formato e o repository não é lido")
    void futureDateOfAnUnknownEquipmentIsABadRequest() {
        var unknown = new UpdateInstalledEquipment.Params(
                ADMINISTRATOR, UUID.randomUUID(), "SN-2", LocalDate.of(2026, 6, 16));

        assertThatThrownBy(() -> update.execute(unknown)).satisfies(domainCode("invalid_installation_date"));

        verifyNoInteractions(repository);
    }

    @Test
    @DisplayName("equipamento que não existe é installed_equipment_not_found")
    void unknownEquipmentIsNotFound() {
        assertThatThrownBy(() -> update.execute(
                new UpdateInstalledEquipment.Params(ADMINISTRATOR, UUID.randomUUID(), "SN-2", null)))
                .satisfies(applicationCode("installed_equipment_not_found"));
    }

    @Test
    @DisplayName("trocar para a própria série em outra caixa não conflita consigo mesmo")
    void updateToItsOwnSerialInAnotherCaseDoesNotConflictWithItself() {
        var serial = SerialNumber.of("sn-1");
        // só o próprio id como exceção evita o conflito: sem exceção o repository responderia true
        when(repository.existsByModelAndSerialNumber(equipment.getModelId(), serial, null)).thenReturn(true);

        update.execute(params("sn-1", null));

        verify(repository).existsByModelAndSerialNumber(equipment.getModelId(), serial, equipment.getId());
        verify(repository).save(equipment);
    }

    @Test
    @DisplayName("série de outro equipamento do modelo é conflito")
    void updateToASerialOfAnotherEquipmentIsAConflict() {
        when(repository.existsByModelAndSerialNumber(equipment.getModelId(), SerialNumber.of("SN-9"), equipment.getId()))
                .thenReturn(true);

        assertThatThrownBy(() -> update.execute(params("SN-9", null)))
                .satisfies(applicationCode("serial_number_taken"));

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("equipamento em local desativado é editado e salvo (ADR-30)")
    void updateOfAnEquipmentInADeactivatedLocationSaves() {
        // o use case só recebe o repository do equipamento: local e cliente nem são lidos
        update.execute(params("SN-2", null));

        verify(repository).save(equipment);
    }

    @Test
    @DisplayName("equipamento desativado não pode ser editado")
    void updateOfADeactivatedEquipmentFails() {
        equipment = equipment(CustomerStatus.DEACTIVATED);
        when(repository.findById(equipment.getId())).thenReturn(Optional.of(equipment));

        assertThatThrownBy(() -> update.execute(params("SN-2", null)))
                .satisfies(domainCode("installed_equipment_deactivated"));

        verify(repository, never()).save(any());
    }
}
