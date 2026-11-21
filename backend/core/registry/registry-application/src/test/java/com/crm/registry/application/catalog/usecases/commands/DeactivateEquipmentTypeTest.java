package com.crm.registry.application.catalog.usecases.commands;

import com.crm.common.application.exceptions.ForbiddenException;
import com.crm.registry.application.catalog.contracts.repositories.EquipmentTypeRepository;
import com.crm.registry.domain.catalog.entities.EquipmentType;
import com.crm.registry.domain.catalog.vos.CatalogStatus;
import com.crm.registry.domain.catalog.vos.CharacteristicName;
import com.crm.registry.domain.catalog.vos.MeasurementUnit;
import com.crm.registry.domain.catalog.vos.TypeName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;
import java.util.UUID;

import static com.crm.registry.application.catalog.CatalogFixtures.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@DisplayName("DeactivateEquipmentType")
class DeactivateEquipmentTypeTest {

    private EquipmentTypeRepository repository;
    private DeactivateEquipmentType deactivateEquipmentType;

    private final EquipmentType type = EquipmentType.register(
            TypeName.of("Inversor"),
            CharacteristicName.of("Potência"),
            MeasurementUnit.KILOWATT,
            true
    );

    @BeforeEach
    void setUp() {
        repository = mock(EquipmentTypeRepository.class);

        when(repository.findById(type.getId())).thenReturn(Optional.of(type));

        deactivateEquipmentType = new DeactivateEquipmentType(repository);
    }

    @Test
    @DisplayName("salva o tipo desativado")
    void savesTheNewStatus() {
        deactivateEquipmentType.execute(new DeactivateEquipmentType.Params(ADMINISTRATOR, type.getId()));

        var saved = ArgumentCaptor.forClass(EquipmentType.class);
        verify(repository).save(saved.capture());

        assertThat(saved.getValue().getId()).isEqualTo(type.getId());
        assertThat(saved.getValue().getStatus()).isEqualTo(CatalogStatus.DEACTIVATED);
    }

    @Test
    @DisplayName("Técnico recebe forbidden e o repository não é tocado")
    void technicianIsForbiddenAndTheRepositoryIsUntouched() {
        assertThatThrownBy(() -> deactivateEquipmentType.execute(
                new DeactivateEquipmentType.Params(TECHNICIAN, type.getId())))
                .isInstanceOf(ForbiddenException.class)
                .satisfies(applicationCode("forbidden"));

        verifyNoInteractions(repository);
    }

    @Test
    @DisplayName("Comercial também pode")
    void commercialCanWrite() {
        deactivateEquipmentType.execute(new DeactivateEquipmentType.Params(COMMERCIAL, type.getId()));

        verify(repository).save(type);
    }

    @Test
    @DisplayName("tipo que não existe é type_not_found")
    void notFound() {
        var unknown = UUID.randomUUID();
        when(repository.findById(unknown)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> deactivateEquipmentType.execute(new DeactivateEquipmentType.Params(ADMINISTRATOR, unknown)))
                .satisfies(applicationCode("type_not_found"));

        verify(repository, never()).save(any());
    }
}
