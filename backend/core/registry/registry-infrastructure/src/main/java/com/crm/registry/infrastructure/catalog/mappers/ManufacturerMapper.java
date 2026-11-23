package com.crm.registry.infrastructure.catalog.mappers;

import com.crm.common.infrastructure.persistence.repositories.EntityMapper;
import com.crm.registry.domain.catalog.entities.Manufacturer;
import com.crm.registry.domain.catalog.vos.ManufacturerName;
import com.crm.registry.infrastructure.catalog.persistence.entities.ManufacturerEntity;

/**
 * Converte {@link Manufacturer} em {@link ManufacturerEntity} e de volta. {@code createdAt} e
 * {@code updatedAt} são só da persistência e nunca chegam ao domínio.
 */
public class ManufacturerMapper implements EntityMapper<Manufacturer, ManufacturerEntity> {

    @Override
    public Manufacturer toDomain(ManufacturerEntity entity) {
        return Manufacturer.reconstitute(
                entity.getId(),
                entity.getVersion(),
                ManufacturerName.of(entity.getName()),
                entity.getStatus()
        );
    }

    @Override
    public ManufacturerEntity toPersistence(Manufacturer manufacturer) {
        var entity = new ManufacturerEntity();

        entity.setId(manufacturer.getId());
        entity.setVersion(manufacturer.getVersion().orElse(null));
        entity.setName(manufacturer.getName().getValue());
        entity.setStatus(manufacturer.getStatus());

        return entity;
    }
}
