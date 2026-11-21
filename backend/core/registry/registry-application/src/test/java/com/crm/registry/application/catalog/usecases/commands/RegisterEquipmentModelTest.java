package com.crm.registry.application.catalog.usecases.commands;

import com.crm.common.application.exceptions.ConflictException;
import com.crm.common.application.exceptions.ForbiddenException;
import com.crm.registry.application.catalog.contracts.repositories.EquipmentModelRepository;
import com.crm.registry.application.catalog.contracts.repositories.EquipmentTypeRepository;
import com.crm.registry.application.catalog.contracts.repositories.ManufacturerRepository;
import com.crm.registry.application.catalog.schemas.ValueInput;
import com.crm.registry.domain.catalog.entities.EquipmentModel;
import com.crm.registry.domain.catalog.entities.EquipmentType;
import com.crm.registry.domain.catalog.entities.Manufacturer;
import com.crm.registry.domain.catalog.vos.CatalogStatus;
import com.crm.registry.domain.catalog.vos.CharacteristicName;
import com.crm.registry.domain.catalog.vos.ManufacturerName;
import com.crm.registry.domain.catalog.vos.MeasurementUnit;
import com.crm.registry.domain.catalog.vos.ModelName;
import com.crm.registry.domain.catalog.vos.TypeName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.crm.registry.application.catalog.CatalogFixtures.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@DisplayName("RegisterEquipmentModel")
class RegisterEquipmentModelTest {

    private EquipmentModelRepository repository;
    private ManufacturerRepository manufacturerRepository;
    private EquipmentTypeRepository typeRepository;
    private RegisterEquipmentModel registerEquipmentModel;

    private final Manufacturer manufacturer = Manufacturer.register(ManufacturerName.of("Huawei"));
    private final EquipmentType type = EquipmentType.register(
            TypeName.of("Inversor"),
            CharacteristicName.of("Potência"),
            MeasurementUnit.KILOWATT,
            true
    );
    private final UUID definitionId = type.getCharacteristic().getId();

    @BeforeEach
    void setUp() {
        repository = mock(EquipmentModelRepository.class);
        manufacturerRepository = mock(ManufacturerRepository.class);
        typeRepository = mock(EquipmentTypeRepository.class);

        when(manufacturerRepository.findById(manufacturer.getId())).thenReturn(Optional.of(manufacturer));
        when(typeRepository.findById(type.getId())).thenReturn(Optional.of(type));

        registerEquipmentModel = new RegisterEquipmentModel(repository, manufacturerRepository, typeRepository);
    }

    private RegisterEquipmentModel.Params params(String name, List<ValueInput> values) {
        return new RegisterEquipmentModel.Params(ADMINISTRATOR, manufacturer.getId(), type.getId(), name, values);
    }

    private List<ValueInput> power(String amount) {
        return List.of(new ValueInput(definitionId, amount));
    }

    @Test
    @DisplayName("salva o modelo ativo com fabricante, tipo e valores, e devolve o id")
    void savesTheModel() {
        var response = registerEquipmentModel.execute(params("SUN2000-5KTL", power("5.5")));

        var saved = ArgumentCaptor.forClass(EquipmentModel.class);
        verify(repository).save(saved.capture());

        var model = saved.getValue();
        assertThat(response.id()).isEqualTo(model.getId());
        assertThat(model.getName()).isEqualTo(ModelName.of("SUN2000-5KTL"));
        assertThat(model.getStatus()).isEqualTo(CatalogStatus.ACTIVE);
        assertThat(model.getManufacturerId()).isEqualTo(manufacturer.getId());
        assertThat(model.getTypeId()).isEqualTo(type.getId());
        assertThat(model.getValues()).singleElement().satisfies(value -> {
            assertThat(value.definitionId()).isEqualTo(definitionId);
            assertThat(value.amount().getValue()).isEqualByComparingTo(new BigDecimal("5.5"));
        });
    }

    @Test
    @DisplayName("Comercial também cadastra")
    void commercialCanWrite() {
        registerEquipmentModel.execute(new RegisterEquipmentModel.Params(
                COMMERCIAL, manufacturer.getId(), type.getId(), "SUN2000", power("5")));

        verify(repository).save(any());
    }

    @Test
    @DisplayName("Técnico recebe forbidden e nenhum repository é tocado")
    void technicianIsForbiddenToWrite() {
        assertThatThrownBy(() -> registerEquipmentModel.execute(new RegisterEquipmentModel.Params(
                TECHNICIAN, manufacturer.getId(), type.getId(), "SUN2000", power("5"))))
                .isInstanceOf(ForbiddenException.class)
                .satisfies(applicationCode("forbidden"));

        verifyNoInteractions(repository, manufacturerRepository, typeRepository);
    }

    @Test
    @DisplayName("fabricante que não existe é manufacturer_not_found")
    void manufacturerNotFound() {
        var unknown = UUID.randomUUID();
        when(manufacturerRepository.findById(unknown)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> registerEquipmentModel.execute(new RegisterEquipmentModel.Params(
                ADMINISTRATOR, unknown, type.getId(), "SUN2000", power("5"))))
                .satisfies(applicationCode("manufacturer_not_found"));

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("tipo que não existe é type_not_found")
    void typeNotFound() {
        var unknown = UUID.randomUUID();
        when(typeRepository.findById(unknown)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> registerEquipmentModel.execute(new RegisterEquipmentModel.Params(
                ADMINISTRATOR, manufacturer.getId(), unknown, "SUN2000", power("5"))))
                .satisfies(applicationCode("type_not_found"));

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("fabricante desativado é manufacturer_deactivated")
    void registerModelRefusesADeactivatedManufacturer() {
        manufacturer.deactivate();

        assertThatThrownBy(() -> registerEquipmentModel.execute(params("SUN2000", power("5"))))
                .satisfies(domainCode("manufacturer_deactivated"));

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("tipo desativado é type_deactivated")
    void registerModelRefusesADeactivatedType() {
        type.deactivate();

        assertThatThrownBy(() -> registerEquipmentModel.execute(params("SUN2000", power("5"))))
                .satisfies(domainCode("type_deactivated"));

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("nome repetido para o mesmo fabricante é model_name_taken e nada é salvo")
    void registerModelRefusesARepeatedNameForTheSameManufacturer() {
        when(repository.existsByManufacturerAndName(manufacturer.getId(), ModelName.of("SUN2000"), null))
                .thenReturn(true);

        assertThatThrownBy(() -> registerEquipmentModel.execute(params("SUN2000", power("5"))))
                .isInstanceOf(ConflictException.class)
                .satisfies(applicationCode("model_name_taken"))
                .hasMessage("este fabricante já tem um modelo com esse nome");

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("o mesmo nome em outro fabricante passa: a checagem leva o id do fabricante do modelo")
    void theSameNameForAnotherManufacturerIsAllowed() {
        var other = Manufacturer.register(ManufacturerName.of("Solis"));
        when(manufacturerRepository.findById(other.getId())).thenReturn(Optional.of(other));
        when(repository.existsByManufacturerAndName(manufacturer.getId(), ModelName.of("SUN2000"), null))
                .thenReturn(true);

        registerEquipmentModel.execute(new RegisterEquipmentModel.Params(
                ADMINISTRATOR, other.getId(), type.getId(), "SUN2000", power("5")));

        verify(repository).existsByManufacturerAndName(other.getId(), ModelName.of("SUN2000"), null);
        verify(repository).save(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "1.2345", "abc", "-1"})
    @DisplayName("valor inválido é invalid_amount e save nunca é chamado")
    void registerModelRefusesAnInvalidAmount(String amount) {
        assertThatThrownBy(() -> registerEquipmentModel.execute(params("SUN2000", power(amount))))
                .satisfies(domainCode("invalid_amount"));

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("valor de uma definição que o tipo não tem é characteristic_unknown")
    void registerModelRefusesAValueOfAnotherType() {
        var foreign = List.of(new ValueInput(UUID.randomUUID(), "5"));

        assertThatThrownBy(() -> registerEquipmentModel.execute(params("SUN2000", foreign)))
                .satisfies(domainCode("characteristic_unknown"));

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("característica obrigatória sem valor é characteristic_required")
    void registerModelRefusesAMissingRequiredValue() {
        assertThatThrownBy(() -> registerEquipmentModel.execute(params("SUN2000", List.of())))
                .satisfies(domainCode("characteristic_required"));

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("lista de valores nula vale vazia: obrigatória recusa, opcional aceita")
    void aNullValuesListIsAnEmptyList() {
        assertThatThrownBy(() -> registerEquipmentModel.execute(params("SUN2000", null)))
                .satisfies(domainCode("characteristic_required"));

        var optional = EquipmentType.register(
                TypeName.of("Painel"), CharacteristicName.of("Potência"), MeasurementUnit.WATT, false);
        when(typeRepository.findById(optional.getId())).thenReturn(Optional.of(optional));

        registerEquipmentModel.execute(new RegisterEquipmentModel.Params(
                ADMINISTRATOR, manufacturer.getId(), optional.getId(), "P550", null));

        verify(repository).save(any());
    }

    @Test
    @DisplayName("fabricante desativado com nome também repetido é manufacturer_deactivated, não o conflito")
    void domainRulesComeBeforeTheNameConflict() {
        manufacturer.deactivate();
        when(repository.existsByManufacturerAndName(manufacturer.getId(), ModelName.of("SUN2000"), null))
                .thenReturn(true);

        assertThatThrownBy(() -> registerEquipmentModel.execute(params("SUN2000", power("5"))))
                .satisfies(domainCode("manufacturer_deactivated"));
    }

    @Test
    @DisplayName("confere o nome livre antes de salvar")
    void checksTheNameBeforeSaving() {
        registerEquipmentModel.execute(params("SUN2000", power("5")));

        var order = inOrder(repository);
        order.verify(repository).existsByManufacturerAndName(any(), any(), any());
        order.verify(repository).save(any());
    }
}
