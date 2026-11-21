package com.crm.registry.application.catalog.usecases.commands;

import com.crm.common.application.exceptions.ConflictException;
import com.crm.common.application.exceptions.ForbiddenException;
import com.crm.registry.application.catalog.contracts.repositories.EquipmentModelRepository;
import com.crm.registry.application.catalog.contracts.repositories.EquipmentTypeRepository;
import com.crm.registry.application.catalog.schemas.ValueInput;
import com.crm.registry.domain.catalog.entities.EquipmentModel;
import com.crm.registry.domain.catalog.entities.EquipmentType;
import com.crm.registry.domain.catalog.vos.CharacteristicAmount;
import com.crm.registry.domain.catalog.vos.CharacteristicName;
import com.crm.registry.domain.catalog.vos.CharacteristicValue;
import com.crm.registry.domain.catalog.vos.CatalogStatus;
import com.crm.registry.domain.catalog.vos.MeasurementUnit;
import com.crm.registry.domain.catalog.vos.ModelName;
import com.crm.registry.domain.catalog.vos.TypeName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
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

@DisplayName("UpdateEquipmentModel")
class UpdateEquipmentModelTest {

    private EquipmentModelRepository repository;
    private EquipmentTypeRepository typeRepository;
    private UpdateEquipmentModel updateEquipmentModel;

    private final EquipmentType type = EquipmentType.register(
            TypeName.of("Inversor"),
            CharacteristicName.of("Potência"),
            MeasurementUnit.KILOWATT,
            true
    );
    private final UUID definitionId = type.getCharacteristic().getId();
    private final UUID manufacturerId = UUID.randomUUID();

    private final EquipmentModel model = EquipmentModel.reconstitute(
            UUID.randomUUID(),
            1L,
            manufacturerId,
            type.getId(),
            ModelName.of("SUN2000"),
            List.of(new CharacteristicValue(definitionId, CharacteristicAmount.of("5"))),
            CatalogStatus.ACTIVE
    );

    @BeforeEach
    void setUp() {
        repository = mock(EquipmentModelRepository.class);
        typeRepository = mock(EquipmentTypeRepository.class);

        when(repository.findById(model.getId())).thenReturn(Optional.of(model));
        when(typeRepository.findById(type.getId())).thenReturn(Optional.of(type));

        updateEquipmentModel = new UpdateEquipmentModel(repository, typeRepository);
    }

    private UpdateEquipmentModel.Params params(String name, String amount) {
        return new UpdateEquipmentModel.Params(
                ADMINISTRATOR,
                model.getId(),
                name,
                List.of(new ValueInput(definitionId, amount))
        );
    }

    @Test
    @DisplayName("troca nome e valores, mantém fabricante e tipo, e salva")
    void updateKeepsManufacturerAndType() {
        updateEquipmentModel.execute(params("SUN2000-8KTL", "8"));

        assertThat(model.getName()).isEqualTo(ModelName.of("SUN2000-8KTL"));
        assertThat(model.getValues()).singleElement()
                .satisfies(value -> assertThat(value.amount().getValue()).isEqualByComparingTo(new BigDecimal("8")));
        assertThat(model.getManufacturerId()).isEqualTo(manufacturerId);
        assertThat(model.getTypeId()).isEqualTo(type.getId());
        verify(repository).save(model);
    }

    @Test
    @DisplayName("o tipo desativado não impede editar o modelo")
    void aDeactivatedTypeStillAllowsEditing() {
        type.deactivate();

        updateEquipmentModel.execute(params("SUN2000-8KTL", "8"));

        verify(repository).save(model);
    }

    @Test
    @DisplayName("a checagem de nome leva o fabricante do modelo e o id do próprio modelo")
    void theNameCheckTargetsTheModelsManufacturerExcludingItself() {
        updateEquipmentModel.execute(params("SUN2000", "8"));

        verify(repository).existsByManufacturerAndName(manufacturerId, ModelName.of("SUN2000"), model.getId());
        verify(repository).save(model);
    }

    @Test
    @DisplayName("nome de outro modelo do fabricante é model_name_taken e nada é salvo")
    void refusesARepeatedName() {
        when(repository.existsByManufacturerAndName(manufacturerId, ModelName.of("OUTRO"), model.getId()))
                .thenReturn(true);

        assertThatThrownBy(() -> updateEquipmentModel.execute(params("OUTRO", "8")))
                .isInstanceOf(ConflictException.class)
                .satisfies(applicationCode("model_name_taken"));

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("modelo desativado é model_deactivated, antes do conflito de nome")
    void updateOfADeactivatedModelFails() {
        model.deactivate();
        when(repository.existsByManufacturerAndName(manufacturerId, ModelName.of("OUTRO"), model.getId()))
                .thenReturn(true);

        assertThatThrownBy(() -> updateEquipmentModel.execute(params("OUTRO", "8")))
                .satisfies(domainCode("model_deactivated"));

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("valor inválido é invalid_amount e save nunca é chamado")
    void invalidAmount() {
        assertThatThrownBy(() -> updateEquipmentModel.execute(params("SUN2000", "1.2345")))
                .satisfies(domainCode("invalid_amount"));

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("tirar o valor de um tipo obrigatório é characteristic_required")
    void missingRequiredValue() {
        assertThatThrownBy(() -> updateEquipmentModel.execute(
                new UpdateEquipmentModel.Params(ADMINISTRATOR, model.getId(), "SUN2000", null)))
                .satisfies(domainCode("characteristic_required"));

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Técnico recebe forbidden e nenhum repository é tocado")
    void technicianIsForbidden() {
        assertThatThrownBy(() -> updateEquipmentModel.execute(new UpdateEquipmentModel.Params(
                TECHNICIAN, model.getId(), "SUN2000", List.of())))
                .isInstanceOf(ForbiddenException.class);

        verifyNoInteractions(repository, typeRepository);
    }

    @Test
    @DisplayName("modelo que não existe é model_not_found")
    void modelNotFound() {
        var unknown = UUID.randomUUID();
        when(repository.findById(unknown)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> updateEquipmentModel.execute(new UpdateEquipmentModel.Params(
                ADMINISTRATOR, unknown, "SUN2000", List.of())))
                .satisfies(applicationCode("model_not_found"));
    }

    @Test
    @DisplayName("tipo do modelo que não existe é type_not_found")
    void typeNotFound() {
        when(typeRepository.findById(type.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> updateEquipmentModel.execute(params("SUN2000", "5")))
                .satisfies(applicationCode("type_not_found"));

        verify(repository, never()).save(any());
    }
}
