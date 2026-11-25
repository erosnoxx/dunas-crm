package com.crm.registry.application.customers.usecases.commands;

import com.crm.common.application.exceptions.ForbiddenException;
import com.crm.registry.application.customers.contracts.repositories.InstalledEquipmentRepository;
import com.crm.registry.domain.customers.entities.InstalledEquipment;
import com.crm.registry.domain.customers.enums.CustomerStatus;
import com.crm.registry.domain.customers.vos.SerialNumber;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static com.crm.registry.application.customers.CustomersFixtures.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@DisplayName("DeactivateInstalledEquipment e ReactivateInstalledEquipment")
class DeactivateAndReactivateInstalledEquipmentTest {

    private InstalledEquipmentRepository repository;

    private InstalledEquipment equipment(CustomerStatus status) {
        var equipment = InstalledEquipment.reconstitute(UUID.randomUUID(), 0L, UUID.randomUUID(), UUID.randomUUID(),
                SerialNumber.of("SN-1"), null, status);
        when(repository.findById(equipment.getId())).thenReturn(Optional.of(equipment));
        return equipment;
    }

    @BeforeEach
    void setUp() {
        repository = mock(InstalledEquipmentRepository.class);
    }

    @Test
    @DisplayName("desativa e salva")
    void deactivates() {
        var equipment = equipment(CustomerStatus.ACTIVE);

        new DeactivateInstalledEquipment(repository)
                .execute(new DeactivateInstalledEquipment.Params(ADMINISTRATOR, equipment.getId()));

        assertThat(equipment.isActive()).isFalse();
        verify(repository).save(equipment);
    }

    @Test
    @DisplayName("reativa e salva")
    void reactivates() {
        var equipment = equipment(CustomerStatus.DEACTIVATED);

        new ReactivateInstalledEquipment(repository)
                .execute(new ReactivateInstalledEquipment.Params(COMMERCIAL, equipment.getId()));

        assertThat(equipment.isActive()).isTrue();
        verify(repository).save(equipment);
    }

    @Test
    @DisplayName("Técnico é proibido nos dois e o repository não é tocado")
    void technicianIsForbidden() {
        var id = UUID.randomUUID();

        assertThatThrownBy(() -> new DeactivateInstalledEquipment(repository)
                .execute(new DeactivateInstalledEquipment.Params(TECHNICIAN, id)))
                .isInstanceOf(ForbiddenException.class);
        assertThatThrownBy(() -> new ReactivateInstalledEquipment(repository)
                .execute(new ReactivateInstalledEquipment.Params(TECHNICIAN, id)))
                .isInstanceOf(ForbiddenException.class);

        verifyNoInteractions(repository);
    }

    @Test
    @DisplayName("equipamento que não existe é installed_equipment_not_found")
    void unknownIsNotFound() {
        assertThatThrownBy(() -> new DeactivateInstalledEquipment(repository)
                .execute(new DeactivateInstalledEquipment.Params(ADMINISTRATOR, UUID.randomUUID())))
                .satisfies(applicationCode("installed_equipment_not_found"));
    }

    @Test
    @DisplayName("desativar um desativado e reativar um ativo falham")
    void invalidTransitionsFail() {
        var deactivated = equipment(CustomerStatus.DEACTIVATED);
        var active = equipment(CustomerStatus.ACTIVE);

        assertThatThrownBy(() -> new DeactivateInstalledEquipment(repository)
                .execute(new DeactivateInstalledEquipment.Params(ADMINISTRATOR, deactivated.getId())))
                .satisfies(domainCode("installed_equipment_deactivated"));
        assertThatThrownBy(() -> new ReactivateInstalledEquipment(repository)
                .execute(new ReactivateInstalledEquipment.Params(ADMINISTRATOR, active.getId())))
                .satisfies(domainCode("installed_equipment_not_deactivated"));
    }
}
