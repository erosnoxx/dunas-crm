package com.crm.registry.infrastructure.catalog.persistence;

import com.crm.common.infrastructure.persistence.repositories.EntityMapper;
import com.crm.registry.domain.catalog.entities.CharacteristicDefinition;
import com.crm.registry.domain.catalog.entities.EquipmentType;
import com.crm.registry.domain.catalog.vos.CharacteristicName;
import com.crm.registry.domain.catalog.vos.TypeName;

import java.util.ArrayList;

/**
 * Converte {@link EquipmentType} em {@link EquipmentTypeEntity} e de volta, com a definição da
 * característica, que mantém o id. A definição não tem versão no domínio: a dela sai nula daqui e
 * o {@link JpaEquipmentTypeRepositoryImpl} a completa com a gravada.
 */
public class EquipmentTypeMapper implements EntityMapper<EquipmentType, EquipmentTypeEntity> {

    @Override
    public EquipmentType toDomain(EquipmentTypeEntity entity) {
        var characteristics = entity.getCharacteristics().stream()
                .map(definition -> CharacteristicDefinition.reconstitute(
                        definition.getId(),
                        CharacteristicName.of(definition.getName()),
                        definition.getUnit(),
                        definition.isRequired()
                ))
                .toList();

        return EquipmentType.reconstitute(
                entity.getId(),
                entity.getVersion(),
                TypeName.of(entity.getName()),
                entity.getStatus(),
                characteristics
        );
    }

    @Override
    public EquipmentTypeEntity toPersistence(EquipmentType type) {
        var entity = new EquipmentTypeEntity();

        entity.setId(type.getId());
        entity.setVersion(type.getVersion().orElse(null));
        entity.setName(type.getName().getValue());
        entity.setStatus(type.getStatus());
        entity.setCharacteristics(new ArrayList<>(type.getCharacteristics().stream()
                .map(this::toPersistence)
                .toList()));

        return entity;
    }

    private CharacteristicDefinitionEntity toPersistence(CharacteristicDefinition definition) {
        var entity = new CharacteristicDefinitionEntity();

        entity.setId(definition.getId());
        entity.setName(definition.getName().getValue());
        entity.setUnit(definition.getUnit());
        entity.setRequired(definition.isRequired());

        return entity;
    }
}
