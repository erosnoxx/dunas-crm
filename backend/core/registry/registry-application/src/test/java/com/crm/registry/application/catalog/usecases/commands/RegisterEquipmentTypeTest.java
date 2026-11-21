package com.crm.registry.application.catalog.usecases.commands;

import com.crm.common.application.exceptions.BadRequestException;
import com.crm.common.application.exceptions.ConflictException;
import com.crm.common.application.exceptions.ForbiddenException;
import com.crm.registry.application.catalog.contracts.repositories.EquipmentTypeRepository;
import com.crm.registry.application.catalog.schemas.CharacteristicInput;
import com.crm.registry.domain.catalog.entities.EquipmentType;
import com.crm.registry.domain.catalog.vos.CatalogStatus;
import com.crm.registry.domain.catalog.vos.MeasurementUnit;
import com.crm.registry.domain.catalog.vos.TypeName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static com.crm.registry.application.catalog.CatalogFixtures.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@DisplayName("RegisterEquipmentType")
class RegisterEquipmentTypeTest {

    private EquipmentTypeRepository repository;
    private RegisterEquipmentType registerEquipmentType;

    @BeforeEach
    void setUp() {
        repository = mock(EquipmentTypeRepository.class);

        registerEquipmentType = new RegisterEquipmentType(repository);
    }

    @Test
    @DisplayName("cadastra um tipo ativo com a característica e devolve o id")
    void registersAndReturnsTheId() {
        var response = registerEquipmentType.execute(new RegisterEquipmentType.Params(ADMINISTRATOR, "Inversor", POWER));

        var saved = ArgumentCaptor.forClass(EquipmentType.class);
        verify(repository).existsByName(TypeName.of("Inversor"), null);
        verify(repository).save(saved.capture());

        var type = saved.getValue();
        assertThat(response.id()).isEqualTo(type.getId());
        assertThat(type.getName()).isEqualTo(TypeName.of("Inversor"));
        assertThat(type.getStatus()).isEqualTo(CatalogStatus.ACTIVE);
        assertThat(type.getCharacteristic().getUnit()).isEqualTo(MeasurementUnit.KILOWATT);
        assertThat(type.getCharacteristic().isRequired()).isTrue();
    }

    @Test
    @DisplayName("Técnico recebe forbidden e o repository não é tocado")
    void technicianIsForbiddenAndTheRepositoryIsUntouched() {
        assertThatThrownBy(() -> registerEquipmentType.execute(new RegisterEquipmentType.Params(TECHNICIAN, "Inversor", POWER)))
                .isInstanceOf(ForbiddenException.class)
                .satisfies(applicationCode("forbidden"));

        verifyNoInteractions(repository);
    }

    @Test
    @DisplayName("Administrador e Comercial podem cadastrar")
    void administratorAndCommercialCanWrite() {
        registerEquipmentType.execute(new RegisterEquipmentType.Params(ADMINISTRATOR, "Inversor", POWER));
        registerEquipmentType.execute(new RegisterEquipmentType.Params(COMMERCIAL, "Painel", POWER));

        verify(repository, times(2)).save(any());
    }

    @Test
    @DisplayName("nome repetido (em outra caixa) é type_name_taken e nada é salvo")
    void refusesARepeatedNameInAnotherCase() {
        when(repository.existsByName(TypeName.of("INVERSOR"), null)).thenReturn(true);

        assertThatThrownBy(() -> registerEquipmentType.execute(new RegisterEquipmentType.Params(COMMERCIAL, "INVERSOR", POWER)))
                .isInstanceOf(ConflictException.class)
                .satisfies(applicationCode("type_name_taken"));

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("unidade desconhecida é 400 no parâmetro unit e nada é salvo")
    void unknownUnitIsABadRequest() {
        var input = new CharacteristicInput("Potência", "kW", true);

        assertThatThrownBy(() -> registerEquipmentType.execute(new RegisterEquipmentType.Params(ADMINISTRATOR, "Inversor", input)))
                .isInstanceOf(BadRequestException.class)
                .satisfies(e -> assertThat(((BadRequestException) e).getInvalidParams())
                        .extracting("name").containsExactly("unit"));

        verify(repository, never()).save(any());
    }
}
