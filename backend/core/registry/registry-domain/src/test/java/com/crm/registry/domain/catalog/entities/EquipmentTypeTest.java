package com.crm.registry.domain.catalog.entities;

import com.crm.common.domain.exceptions.DomainException;
import com.crm.registry.domain.catalog.vos.CatalogStatus;
import com.crm.registry.domain.catalog.vos.CharacteristicAmount;
import com.crm.registry.domain.catalog.vos.CharacteristicName;
import com.crm.registry.domain.catalog.vos.CharacteristicValue;
import com.crm.registry.domain.catalog.vos.MeasurementUnit;
import com.crm.registry.domain.catalog.vos.TypeName;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("EquipmentType")
class EquipmentTypeTest {

    private static final TypeName INVERTER = TypeName.of("Inversor");
    private static final CharacteristicName POWER = CharacteristicName.of("Potência");
    private static final CharacteristicName CAPACITY = CharacteristicName.of("Capacidade");

    private static final String LOCKED_MESSAGE =
            "o tipo já tem modelos: a unidade e a obrigatoriedade não podem mudar";
    private static final String DEACTIVATED_MESSAGE = "este tipo está desativado";
    private static final String UNKNOWN_MESSAGE = "a característica informada não existe neste tipo";

    private static void assertFailsWith(Runnable action, String code, String message) {
        assertThatThrownBy(action::run)
                .isInstanceOf(DomainException.class)
                .satisfies(e -> {
                    assertThat(((DomainException) e).getCode()).contains(code);
                    assertThat(e).hasMessage(message);
                });
    }

    private static EquipmentType inverter(boolean required) {
        return EquipmentType.register(INVERTER, POWER, MeasurementUnit.KILOWATT, required);
    }

    private static CharacteristicValue value(UUID definitionId) {
        return new CharacteristicValue(definitionId, CharacteristicAmount.of(new BigDecimal("5")));
    }

    private static void assertCharacteristic(
            EquipmentType type,
            CharacteristicName name,
            MeasurementUnit unit,
            boolean required
    ) {
        var characteristic = type.getCharacteristic();

        assertThat(characteristic.getName()).isEqualTo(name);
        assertThat(characteristic.getUnit()).isEqualTo(unit);
        assertThat(characteristic.isRequired()).isEqualTo(required);
    }

    @Test
    @DisplayName("nasce ativo, sem versão e com uma única característica")
    void registersWithOneCharacteristic() {
        var type = inverter(true);

        assertThat(type.getId()).isNotNull();
        assertThat(type.getName()).isEqualTo(INVERTER);
        assertThat(type.getStatus()).isEqualTo(CatalogStatus.ACTIVE);
        assertThat(type.isActive()).isTrue();
        assertThat(type.getVersion()).isEmpty();
        assertThat(type.getCharacteristics()).hasSize(1);
        assertThat(type.getCharacteristic().getId()).isNotNull();
        assertCharacteristic(type, POWER, MeasurementUnit.KILOWATT, true);
    }

    @Test
    @DisplayName("sem modelos, troca nome, unidade e obrigatoriedade da característica mantendo o id")
    void changeCharacteristicWithoutModelsChangesUnitAndRequired() {
        var type = inverter(false);
        var definitionId = type.getCharacteristic().getId();

        type.changeCharacteristic(CAPACITY, MeasurementUnit.BTU_PER_HOUR, true, false);

        assertThat(type.getCharacteristic().getId()).isEqualTo(definitionId);
        assertCharacteristic(type, CAPACITY, MeasurementUnit.BTU_PER_HOUR, true);
    }

    @Test
    @DisplayName("com modelos, recusa uma unidade nova e não muda nada")
    void changeCharacteristicWithModelsRefusesANewUnit() {
        var type = inverter(true);

        assertFailsWith(
                () -> type.changeCharacteristic(CAPACITY, MeasurementUnit.WATT, true, true),
                "characteristic_locked",
                LOCKED_MESSAGE
        );
        assertCharacteristic(type, POWER, MeasurementUnit.KILOWATT, true);
    }

    @Test
    @DisplayName("com modelos, recusa tornar a característica obrigatória e não muda nada")
    void changeCharacteristicWithModelsRefusesTurningRequiredOn() {
        var type = inverter(false);

        assertFailsWith(
                () -> type.changeCharacteristic(CAPACITY, MeasurementUnit.KILOWATT, true, true),
                "characteristic_locked",
                LOCKED_MESSAGE
        );
        assertCharacteristic(type, POWER, MeasurementUnit.KILOWATT, false);
    }

    @Test
    @DisplayName("com modelos, aceita um nome novo e deixar de ser obrigatória")
    void changeCharacteristicWithModelsAcceptsANewNameAndTurningRequiredOff() {
        var type = inverter(true);

        type.changeCharacteristic(CAPACITY, MeasurementUnit.KILOWATT, false, true);

        assertCharacteristic(type, CAPACITY, MeasurementUnit.KILOWATT, false);
    }

    @Test
    @DisplayName("com modelos, aceita manter tudo como está")
    void changeCharacteristicWithModelsAcceptsNoChange() {
        var type = inverter(true);

        type.changeCharacteristic(POWER, MeasurementUnit.KILOWATT, true, true);

        assertCharacteristic(type, POWER, MeasurementUnit.KILOWATT, true);
    }

    @Test
    @DisplayName("renomeia um tipo ativo")
    void renameChangesTheName() {
        var type = inverter(true);

        type.rename(TypeName.of("Ar-condicionado"));

        assertThat(type.getName()).isEqualTo(TypeName.of("Ar-condicionado"));
    }

    @Test
    @DisplayName("recusa renomear, desativar e alterar a característica de um tipo desativado")
    void renameAndDeactivateOfADeactivatedTypeFail() {
        var type = inverter(true);
        type.deactivate();

        assertFailsWith(() -> type.rename(TypeName.of("Ar-condicionado")), "type_deactivated", DEACTIVATED_MESSAGE);
        assertFailsWith(type::deactivate, "type_deactivated", DEACTIVATED_MESSAGE);
        assertFailsWith(
                () -> type.changeCharacteristic(CAPACITY, MeasurementUnit.WATT, false, false),
                "type_deactivated",
                DEACTIVATED_MESSAGE
        );
        assertThat(type.getName()).isEqualTo(INVERTER);
        assertThat(type.getStatus()).isEqualTo(CatalogStatus.DEACTIVATED);
        assertCharacteristic(type, POWER, MeasurementUnit.KILOWATT, true);
    }

    @Test
    @DisplayName("recusa reativar um tipo ativo")
    void reactivateAnActiveOneFails() {
        var type = inverter(true);

        assertFailsWith(type::reactivate, "type_not_deactivated", "este tipo não está desativado");
        assertThat(type.isActive()).isTrue();
    }

    @Test
    @DisplayName("desativa e reativa o tipo")
    void deactivateAndReactivate() {
        var type = inverter(true);

        type.deactivate();
        assertThat(type.isActive()).isFalse();

        type.reactivate();
        assertThat(type.getStatus()).isEqualTo(CatalogStatus.ACTIVE);
    }

    @Test
    @DisplayName("reconstitui mantendo id, versão, nome, situação e a característica")
    void reconstituteKeepsEverything() {
        var id = UUID.randomUUID();
        var definition = CharacteristicDefinition.reconstitute(UUID.randomUUID(), POWER, MeasurementUnit.WATT, false);

        var type = EquipmentType.reconstitute(id, 4L, INVERTER, CatalogStatus.DEACTIVATED, List.of(definition));

        assertThat(type.getId()).isEqualTo(id);
        assertThat(type.getVersion()).contains(4L);
        assertThat(type.getName()).isEqualTo(INVERTER);
        assertThat(type.getStatus()).isEqualTo(CatalogStatus.DEACTIVATED);
        assertThat(type.getCharacteristic()).isSameAs(definition);
    }

    @Test
    @DisplayName("recusa reconstituir com zero ou duas características")
    void reconstituteRefusesZeroOrTwoCharacteristics() {
        var one = CharacteristicDefinition.create(POWER, MeasurementUnit.KILOWATT, true);
        var two = CharacteristicDefinition.create(CAPACITY, MeasurementUnit.WATT, false);
        var id = UUID.randomUUID();

        assertThatThrownBy(() -> EquipmentType.reconstitute(id, 1L, INVERTER, CatalogStatus.ACTIVE, List.of()))
                .isInstanceOf(DomainException.class);
        assertThatThrownBy(() -> EquipmentType.reconstitute(id, 1L, INVERTER, CatalogStatus.ACTIVE, List.of(one, two)))
                .isInstanceOf(DomainException.class);
    }

    @Test
    @DisplayName("aceita o valor da definição do tipo")
    void checkValuesAcceptsTheRightDefinition() {
        var type = inverter(true);

        type.checkValues(List.of(value(type.getCharacteristic().getId())));
    }

    @Test
    @DisplayName("recusa o valor de outra definição")
    void checkValuesRefusesAnotherDefinition() {
        var type = inverter(false);

        assertFailsWith(
                () -> type.checkValues(List.of(value(UUID.randomUUID()))),
                "characteristic_unknown",
                UNKNOWN_MESSAGE
        );
    }

    @Test
    @DisplayName("recusa a mesma definição repetida")
    void checkValuesRefusesARepeatedDefinition() {
        var type = inverter(true);
        var id = type.getCharacteristic().getId();

        assertFailsWith(
                () -> type.checkValues(List.of(value(id), value(id))),
                "characteristic_unknown",
                UNKNOWN_MESSAGE
        );
    }

    @Test
    @DisplayName("recusa a falta do valor de uma característica obrigatória, com o nome na mensagem")
    void checkValuesRefusesAMissingRequiredValue() {
        var type = inverter(true);

        assertFailsWith(
                () -> type.checkValues(List.of()),
                "characteristic_required",
                "informe a característica Potência"
        );
    }

    @Test
    @DisplayName("aceita nenhum valor quando a característica não é obrigatória")
    void checkValuesAcceptsNoValueWhenNotRequired() {
        inverter(false).checkValues(List.of());
    }
}
