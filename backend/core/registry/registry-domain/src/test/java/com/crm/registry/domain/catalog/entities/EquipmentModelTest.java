package com.crm.registry.domain.catalog.entities;

import com.crm.common.domain.exceptions.DomainException;
import com.crm.registry.domain.catalog.vos.CatalogStatus;
import com.crm.registry.domain.catalog.vos.CharacteristicAmount;
import com.crm.registry.domain.catalog.vos.CharacteristicName;
import com.crm.registry.domain.catalog.vos.CharacteristicValue;
import com.crm.registry.domain.catalog.vos.ManufacturerName;
import com.crm.registry.domain.catalog.vos.MeasurementUnit;
import com.crm.registry.domain.catalog.vos.ModelName;
import com.crm.registry.domain.catalog.vos.TypeName;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("EquipmentModel")
class EquipmentModelTest {

    private static final ModelName SUN_5K = ModelName.of("SUN2000-5K");
    private static final ModelName SUN_10K = ModelName.of("SUN2000-10K");

    private static final String MODEL_DEACTIVATED = "este modelo está desativado";

    private static void assertFailsWith(Runnable action, String code, String message) {
        assertThatThrownBy(action::run)
                .isInstanceOf(DomainException.class)
                .satisfies(e -> {
                    assertThat(((DomainException) e).getCode()).contains(code);
                    assertThat(e).hasMessage(message);
                });
    }

    private static Manufacturer manufacturer() {
        return Manufacturer.register(ManufacturerName.of("Huawei"));
    }

    private static EquipmentType type(boolean required) {
        return EquipmentType.register(
                TypeName.of("Inversor"),
                CharacteristicName.of("Potência"),
                MeasurementUnit.KILOWATT,
                required
        );
    }

    private static CharacteristicValue value(EquipmentType type, String amount) {
        return new CharacteristicValue(
                type.getCharacteristic().getId(),
                CharacteristicAmount.of(new BigDecimal(amount))
        );
    }

    @Test
    @DisplayName("nasce ativo, sem versão, com o fabricante, o tipo e os valores")
    void registersActiveWithTheValues() {
        var manufacturer = manufacturer();
        var type = type(true);
        var values = List.of(value(type, "5"));

        var model = EquipmentModel.register(manufacturer, type, SUN_5K, values);

        assertThat(model.getId()).isNotNull();
        assertThat(model.getVersion()).isEmpty();
        assertThat(model.getManufacturerId()).isEqualTo(manufacturer.getId());
        assertThat(model.getTypeId()).isEqualTo(type.getId());
        assertThat(model.getName()).isEqualTo(SUN_5K);
        assertThat(model.getValues()).containsExactlyElementsOf(values);
        assertThat(model.getStatus()).isEqualTo(CatalogStatus.ACTIVE);
        assertThat(model.isActive()).isTrue();
    }

    @Test
    @DisplayName("recusa cadastrar com um fabricante desativado")
    void registerRefusesADeactivatedManufacturer() {
        var manufacturer = manufacturer();
        manufacturer.deactivate();
        var type = type(false);

        assertFailsWith(
                () -> EquipmentModel.register(manufacturer, type, SUN_5K, List.of()),
                "manufacturer_deactivated",
                "este fabricante está desativado"
        );
    }

    @Test
    @DisplayName("recusa cadastrar com um tipo desativado")
    void registerRefusesADeactivatedType() {
        var manufacturer = manufacturer();
        var type = type(false);
        type.deactivate();

        assertFailsWith(
                () -> EquipmentModel.register(manufacturer, type, SUN_5K, List.of()),
                "type_deactivated",
                "este tipo está desativado"
        );
    }

    @Test
    @DisplayName("recusa cadastrar com valores que não casam com o tipo")
    void registerRefusesValuesThatDoNotMatchTheType() {
        var manufacturer = manufacturer();
        var type = type(true);

        assertFailsWith(
                () -> EquipmentModel.register(manufacturer, type, SUN_5K, List.of()),
                "characteristic_required",
                "informe a característica Potência"
        );
    }

    @Test
    @DisplayName("edita nome e valores, nunca o fabricante nem o tipo")
    void editChangesNameAndValuesButNeverManufacturerNorType() {
        var manufacturer = manufacturer();
        var type = type(true);
        var model = EquipmentModel.register(manufacturer, type, SUN_5K, List.of(value(type, "5")));
        var newValues = List.of(value(type, "10"));

        model.edit(type, SUN_10K, newValues);

        assertThat(model.getName()).isEqualTo(SUN_10K);
        assertThat(model.getValues()).containsExactlyElementsOf(newValues);
        assertThat(model.getManufacturerId()).isEqualTo(manufacturer.getId());
        assertThat(model.getTypeId()).isEqualTo(type.getId());
    }

    @Test
    @DisplayName("recusa editar com valores inválidos e não muda nada")
    void editRefusesInvalidValuesAndChangesNothing() {
        var type = type(true);
        var values = List.of(value(type, "5"));
        var model = EquipmentModel.register(manufacturer(), type, SUN_5K, values);

        assertFailsWith(
                () -> model.edit(type, SUN_10K, List.of()),
                "characteristic_required",
                "informe a característica Potência"
        );
        assertThat(model.getName()).isEqualTo(SUN_5K);
        assertThat(model.getValues()).containsExactlyElementsOf(values);
    }

    @Test
    @DisplayName("recusa editar um modelo desativado e não muda nada")
    void editOfADeactivatedModelFails() {
        var type = type(false);
        var model = EquipmentModel.register(manufacturer(), type, SUN_5K, List.of());
        model.deactivate();

        assertFailsWith(() -> model.edit(type, SUN_10K, List.of()), "model_deactivated", MODEL_DEACTIVATED);
        assertThat(model.getName()).isEqualTo(SUN_5K);
        assertThat(model.getStatus()).isEqualTo(CatalogStatus.DEACTIVATED);
    }

    @Test
    @DisplayName("aceita editar quando o tipo está desativado, só o modelo novo é bloqueado")
    void editAcceptsADeactivatedType() {
        var type = type(false);
        var model = EquipmentModel.register(manufacturer(), type, SUN_5K, List.of());
        type.deactivate();

        model.edit(type, SUN_10K, List.of());

        assertThat(model.getName()).isEqualTo(SUN_10K);
    }

    @Test
    @DisplayName("recusa editar com outro tipo e não muda nada")
    void editRefusesAnotherType() {
        var type = type(false);
        var model = EquipmentModel.register(manufacturer(), type, SUN_5K, List.of());

        assertFailsWith(
                () -> model.edit(type(false), SUN_10K, List.of()),
                "type_mismatch",
                "o tipo informado não é o deste modelo"
        );
        assertThat(model.getName()).isEqualTo(SUN_5K);
        assertThat(model.getTypeId()).isEqualTo(type.getId());
    }

    @Test
    @DisplayName("desativa e reativa o modelo, recusando as transições fora de lugar")
    void deactivateAndReactivate() {
        var model = EquipmentModel.register(manufacturer(), type(false), SUN_5K, List.of());

        assertFailsWith(model::reactivate, "model_not_deactivated", "este modelo não está desativado");
        assertThat(model.isActive()).isTrue();

        model.deactivate();
        assertThat(model.isActive()).isFalse();
        assertFailsWith(model::deactivate, "model_deactivated", MODEL_DEACTIVATED);
        assertThat(model.getStatus()).isEqualTo(CatalogStatus.DEACTIVATED);

        model.reactivate();
        assertThat(model.getStatus()).isEqualTo(CatalogStatus.ACTIVE);
    }

    @Test
    @DisplayName("devolve os valores numa cópia que não pode ser alterada")
    void getValuesCannotBeChanged() {
        var type = type(true);
        var input = new ArrayList<>(List.of(value(type, "5")));
        var model = EquipmentModel.register(manufacturer(), type, SUN_5K, input);

        input.clear();

        assertThat(model.getValues()).hasSize(1);
        assertThatThrownBy(() -> model.getValues().clear()).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("reconstitui mantendo id, versão, fabricante, tipo, nome, valores e situação")
    void reconstituteKeepsEverything() {
        var id = UUID.randomUUID();
        var manufacturerId = UUID.randomUUID();
        var typeId = UUID.randomUUID();
        var values = List.of(new CharacteristicValue(UUID.randomUUID(), CharacteristicAmount.of(new BigDecimal("3"))));

        var model = EquipmentModel.reconstitute(
                id,
                2L,
                manufacturerId,
                typeId,
                SUN_5K,
                values,
                CatalogStatus.DEACTIVATED
        );

        assertThat(model.getId()).isEqualTo(id);
        assertThat(model.getVersion()).contains(2L);
        assertThat(model.getManufacturerId()).isEqualTo(manufacturerId);
        assertThat(model.getTypeId()).isEqualTo(typeId);
        assertThat(model.getName()).isEqualTo(SUN_5K);
        assertThat(model.getValues()).containsExactlyElementsOf(values);
        assertThat(model.getStatus()).isEqualTo(CatalogStatus.DEACTIVATED);
    }
}
