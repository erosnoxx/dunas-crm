package com.crm.registry.infrastructure.catalog.persistence;

import com.crm.registry.domain.catalog.entities.EquipmentModel;
import com.crm.registry.domain.catalog.enums.CatalogStatus;
import com.crm.registry.domain.catalog.vos.CharacteristicAmount;
import com.crm.registry.domain.catalog.vos.CharacteristicValue;
import com.crm.registry.domain.catalog.vos.ModelName;
import com.crm.registry.infrastructure.catalog.mappers.EquipmentModelMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("EquipmentModelMapper")
class EquipmentModelMapperTest {

    private final EquipmentModelMapper mapper = new EquipmentModelMapper();

    @Test
    @DisplayName("ida e volta preserva todos os campos, a versão e os valores")
    void roundTripKeepsEveryFieldAndTheValues() {
        var value = new CharacteristicValue(UUID.randomUUID(), CharacteristicAmount.of("5.5"));
        var model = EquipmentModel.reconstitute(
                UUID.randomUUID(),
                3L,
                UUID.randomUUID(),
                UUID.randomUUID(),
                ModelName.of("SUN2000-5KTL"),
                List.of(value),
                CatalogStatus.DEACTIVATED
        );

        var back = mapper.toDomain(mapper.toPersistence(model));

        assertThat(back.getId()).isEqualTo(model.getId());
        assertThat(back.getVersion()).contains(3L);
        assertThat(back.getManufacturerId()).isEqualTo(model.getManufacturerId());
        assertThat(back.getTypeId()).isEqualTo(model.getTypeId());
        assertThat(back.getName()).isEqualTo(model.getName());
        assertThat(back.getStatus()).isEqualTo(CatalogStatus.DEACTIVATED);
        assertThat(back.getValues()).containsExactly(value);
    }

    @Test
    @DisplayName("versão nula é linha nova e modelo sem valor volta sem valor")
    void nullVersionIsNewRowWithoutValues() {
        var model = EquipmentModel.reconstitute(
                UUID.randomUUID(),
                null,
                UUID.randomUUID(),
                UUID.randomUUID(),
                ModelName.of("S6-GR1P3K"),
                List.of(),
                CatalogStatus.ACTIVE
        );

        var entity = mapper.toPersistence(model);

        assertThat(entity.getVersion()).isNull();

        var back = mapper.toDomain(entity);

        assertThat(back.getVersion()).isEmpty();
        assertThat(back.getValues()).isEmpty();
    }
}
