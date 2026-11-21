package com.crm.registry.infrastructure.catalog.persistence;

import com.crm.registry.domain.catalog.entities.CharacteristicDefinition;
import com.crm.registry.domain.catalog.entities.EquipmentType;
import com.crm.registry.domain.catalog.vos.CatalogStatus;
import com.crm.registry.domain.catalog.vos.CharacteristicName;
import com.crm.registry.domain.catalog.vos.MeasurementUnit;
import com.crm.registry.domain.catalog.vos.TypeName;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("EquipmentTypeMapper")
class EquipmentTypeMapperTest {

    private final EquipmentTypeMapper mapper = new EquipmentTypeMapper();

    @Test
    @DisplayName("ida e volta preserva todos os campos, a versão e a definição com o mesmo id")
    void roundTripKeepsEveryFieldAndTheDefinition() {
        var definitionId = UUID.randomUUID();
        var type = EquipmentType.reconstitute(
                UUID.randomUUID(),
                2L,
                TypeName.of("Inversor"),
                CatalogStatus.ACTIVE,
                List.of(CharacteristicDefinition.reconstitute(
                        definitionId,
                        CharacteristicName.of("Potência"),
                        MeasurementUnit.KILOWATT,
                        true
                ))
        );

        var back = mapper.toDomain(mapper.toPersistence(type));

        assertThat(back.getId()).isEqualTo(type.getId());
        assertThat(back.getVersion()).contains(2L);
        assertThat(back.getName()).isEqualTo(type.getName());
        assertThat(back.getStatus()).isEqualTo(CatalogStatus.ACTIVE);
        assertThat(back.getCharacteristic().getId()).isEqualTo(definitionId);
        assertThat(back.getCharacteristic().getName()).isEqualTo(CharacteristicName.of("Potência"));
        assertThat(back.getCharacteristic().getUnit()).isEqualTo(MeasurementUnit.KILOWATT);
        assertThat(back.getCharacteristic().isRequired()).isTrue();
    }

    @Test
    @DisplayName("versão nula é linha nova")
    void nullVersionIsNewRow() {
        var type = EquipmentType.register(
                TypeName.of("Ar-condicionado"),
                CharacteristicName.of("Capacidade"),
                MeasurementUnit.BTU_PER_HOUR,
                false
        );

        var entity = mapper.toPersistence(type);

        assertThat(entity.getVersion()).isNull();
        assertThat(entity.getCharacteristics()).hasSize(1);
        assertThat(mapper.toDomain(entity).getVersion()).isEmpty();
    }
}
