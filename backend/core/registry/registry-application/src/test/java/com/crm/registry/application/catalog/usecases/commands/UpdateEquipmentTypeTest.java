package com.crm.registry.application.catalog.usecases.commands;

import com.crm.common.application.exceptions.BadRequestException;
import com.crm.common.application.exceptions.ConflictException;
import com.crm.common.application.exceptions.ForbiddenException;
import com.crm.common.domain.exceptions.DomainException;
import com.crm.registry.application.catalog.contracts.readers.EquipmentModelReader;
import com.crm.registry.application.catalog.contracts.repositories.EquipmentTypeRepository;
import com.crm.registry.application.catalog.schemas.CharacteristicInput;
import com.crm.registry.domain.catalog.entities.EquipmentType;
import com.crm.registry.domain.catalog.vos.CharacteristicName;
import com.crm.registry.domain.catalog.enums.MeasurementUnit;
import com.crm.registry.domain.catalog.vos.TypeName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

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

@DisplayName("UpdateEquipmentType")
class UpdateEquipmentTypeTest {

    private EquipmentTypeRepository repository;
    private EquipmentModelReader modelReader;
    private UpdateEquipmentType updateEquipmentType;

    private final EquipmentType type = EquipmentType.register(
            TypeName.of("Inversor"),
            CharacteristicName.of("Potência"),
            MeasurementUnit.KILOWATT,
            true
    );

    @BeforeEach
    void setUp() {
        repository = mock(EquipmentTypeRepository.class);
        modelReader = mock(EquipmentModelReader.class);

        when(repository.findById(type.getId())).thenReturn(Optional.of(type));

        updateEquipmentType = new UpdateEquipmentType(repository, modelReader);
    }

    private UpdateEquipmentType.Params params(String name, String unit, boolean required) {
        return new UpdateEquipmentType.Params(
                ADMINISTRATOR,
                type.getId(),
                name,
                new CharacteristicInput("Potência nominal", unit, required)
        );
    }

    @Test
    @DisplayName("sem modelos, troca nome, característica e unidade com um único save")
    void updateWithoutModelsChangesTheUnit() {
        updateEquipmentType.execute(params("Inversor String", "WATT", false));

        assertThat(type.getName()).isEqualTo(TypeName.of("Inversor String"));
        assertThat(type.getCharacteristic().getName()).isEqualTo(CharacteristicName.of("Potência nominal"));
        assertThat(type.getCharacteristic().getUnit()).isEqualTo(MeasurementUnit.WATT);
        assertThat(type.getCharacteristic().isRequired()).isFalse();
        verify(repository).save(type);
    }

    @Test
    @DisplayName("com modelos, nova unidade é characteristic_locked e nada é salvo")
    void updateWithModelsRefusesANewUnitAndSavesNothing() {
        when(modelReader.hasModelsOfType(type.getId())).thenReturn(true);

        assertThatThrownBy(() -> updateEquipmentType.execute(params("Inversor", "WATT", true)))
                .isInstanceOf(DomainException.class)
                .satisfies(e -> assertThat(((DomainException) e).getCode()).contains("characteristic_locked"));

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("com modelos, desligar a obrigatoriedade passa")
    void updateWithModelsAcceptsTurningRequiredOff() {
        when(modelReader.hasModelsOfType(type.getId())).thenReturn(true);

        updateEquipmentType.execute(params("Inversor", "KILOWATT", false));

        assertThat(type.getCharacteristic().isRequired()).isFalse();
        verify(repository).save(type);
    }

    @Test
    @DisplayName("Técnico recebe forbidden e nem o repository nem o reader são tocados")
    void technicianIsForbidden() {
        assertThatThrownBy(() -> updateEquipmentType.execute(
                new UpdateEquipmentType.Params(TECHNICIAN, type.getId(), "Outro", POWER)))
                .isInstanceOf(ForbiddenException.class)
                .satisfies(applicationCode("forbidden"));

        verifyNoInteractions(repository, modelReader);
    }

    @Test
    @DisplayName("nome usado por outro tipo (em outra caixa) é type_name_taken e nada é salvo")
    void refusesARepeatedNameInAnotherCase() {
        when(repository.existsByName(TypeName.of("PAINEL"), type.getId())).thenReturn(true);

        assertThatThrownBy(() -> updateEquipmentType.execute(params("PAINEL", "KILOWATT", true)))
                .isInstanceOf(ConflictException.class)
                .satisfies(applicationCode("type_name_taken"));

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("renomear para o mesmo nome não conflita consigo: o repository recebe o id do próprio tipo")
    void renameToTheSameNameDoesNotConflictWithItself() {
        updateEquipmentType.execute(params("INVERSOR", "KILOWATT", true));

        verify(repository).existsByName(TypeName.of("INVERSOR"), type.getId());
        verify(repository).save(type);
    }

    @Test
    @DisplayName("tipo desativado com nome também repetido é type_deactivated, não o conflito")
    void deactivatedIsReportedBeforeTheNameConflict() {
        type.deactivate();
        when(repository.existsByName(TypeName.of("PAINEL"), type.getId())).thenReturn(true);

        assertThatThrownBy(() -> updateEquipmentType.execute(params("PAINEL", "KILOWATT", true)))
                .isInstanceOf(DomainException.class)
                .satisfies(e -> assertThat(((DomainException) e).getCode()).contains("type_deactivated"));

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("nome repetido é type_name_taken antes de characteristic_locked")
    void nameTakenIsReportedBeforeTheLock() {
        when(modelReader.hasModelsOfType(type.getId())).thenReturn(true);
        when(repository.existsByName(TypeName.of("PAINEL"), type.getId())).thenReturn(true);

        assertThatThrownBy(() -> updateEquipmentType.execute(params("PAINEL", "WATT", true)))
                .isInstanceOf(ConflictException.class)
                .satisfies(applicationCode("type_name_taken"));

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("unidade desconhecida é 400 no parâmetro unit e nada é salvo")
    void unknownUnitIsABadRequest() {
        assertThatThrownBy(() -> updateEquipmentType.execute(params("Inversor", "kW", true)))
                .isInstanceOf(BadRequestException.class)
                .satisfies(e -> assertThat(((BadRequestException) e).getInvalidParams())
                        .extracting("name").containsExactly("unit"));

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("tipo que não existe é type_not_found")
    void typeNotFound() {
        var unknown = UUID.randomUUID();
        when(repository.findById(unknown)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> updateEquipmentType.execute(
                new UpdateEquipmentType.Params(ADMINISTRATOR, unknown, "Novo", POWER)))
                .satisfies(applicationCode("type_not_found"));

        verify(repository, never()).save(any());
    }
}
