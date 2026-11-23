package com.crm.registry.infrastructure.catalog.mappers;

import com.crm.common.infrastructure.persistence.repositories.EntityMapper;
import com.crm.registry.domain.catalog.entities.EquipmentModel;
import com.crm.registry.domain.catalog.vos.CharacteristicAmount;
import com.crm.registry.domain.catalog.vos.CharacteristicValue;
import com.crm.registry.domain.catalog.vos.ModelName;
import com.crm.registry.infrastructure.catalog.persistence.entities.EquipmentModelEntity;
import com.crm.registry.infrastructure.catalog.persistence.entities.ModelCharacteristicValueEntity;

import java.util.ArrayList;

/**
 * Converte {@link EquipmentModel} em {@link EquipmentModelEntity} e de volta, com os valores da
 * característica. {@code createdAt} e {@code updatedAt} nunca chegam ao domínio.
 */
public class EquipmentModelMapper implements EntityMapper<EquipmentModel, EquipmentModelEntity> {

    @Override
    public EquipmentModel toDomain(EquipmentModelEntity entity) {
        var values = entity.getValues().stream()
                .map(value -> new CharacteristicValue(value.getDefinitionId(), CharacteristicAmount.of(value.getAmount())))
                .toList();

        return EquipmentModel.reconstitute(
                entity.getId(),
                entity.getVersion(),
                entity.getManufacturerId(),
                entity.getTypeId(),
                ModelName.of(entity.getName()),
                values,
                entity.getStatus()
        );
    }

    @Override
    public EquipmentModelEntity toPersistence(EquipmentModel model) {
        var entity = new EquipmentModelEntity();

        entity.setId(model.getId());
        entity.setVersion(model.getVersion().orElse(null));
        entity.setManufacturerId(model.getManufacturerId());
        entity.setTypeId(model.getTypeId());
        entity.setName(model.getName().getValue());
        entity.setStatus(model.getStatus());
        entity.setValues(new ArrayList<>(model.getValues().stream()
                .map(value -> new ModelCharacteristicValueEntity(value.definitionId(), value.amount().getValue()))
                .toList()));

        return entity;
    }
}
