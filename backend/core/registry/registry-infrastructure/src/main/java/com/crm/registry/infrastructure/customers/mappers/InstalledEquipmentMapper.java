package com.crm.registry.infrastructure.customers.mappers;

import com.crm.common.domain.vos.LocalDateValueObject;
import com.crm.common.infrastructure.persistence.repositories.EntityMapper;
import com.crm.registry.domain.customers.entities.InstalledEquipment;
import com.crm.registry.domain.customers.vos.InstallationDate;
import com.crm.registry.domain.customers.vos.SerialNumber;
import com.crm.registry.infrastructure.customers.persistence.entities.InstalledEquipmentEntity;

import java.time.Clock;
import java.time.ZoneOffset;

/**
 * Converte {@link InstalledEquipment} em {@link InstalledEquipmentEntity} e de volta. Data de
 * instalação ausente vira coluna nula; {@code createdAt} e {@code updatedAt} nunca chegam ao domínio.
 *
 * <p>A data gravada já foi validada ao entrar; ao ler, ela é reconstruída com um relógio fixado no
 * próprio dia, para que a regra "não pode ser futura" não reprove um dado já aceito.
 */
public class InstalledEquipmentMapper implements EntityMapper<InstalledEquipment, InstalledEquipmentEntity> {

    @Override
    public InstalledEquipment toDomain(InstalledEquipmentEntity entity) {
        var day = entity.getInstalledOn();
        var clock = day == null ? null : Clock.fixed(day.atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC);

        return InstalledEquipment.reconstitute(
                entity.getId(),
                entity.getVersion(),
                entity.getLocationId(),
                entity.getModelId(),
                SerialNumber.of(entity.getSerialNumber()),
                day == null ? null : InstallationDate.of(day, clock),
                entity.getStatus()
        );
    }

    @Override
    public InstalledEquipmentEntity toPersistence(InstalledEquipment equipment) {
        var entity = new InstalledEquipmentEntity();

        entity.setId(equipment.getId());
        entity.setVersion(equipment.getVersion().orElse(null));
        entity.setLocationId(equipment.getLocationId());
        entity.setModelId(equipment.getModelId());
        entity.setSerialNumber(equipment.getSerialNumber().getValue());
        entity.setInstalledOn(equipment.getInstalledOn().map(LocalDateValueObject::getValue).orElse(null));
        entity.setStatus(equipment.getStatus());

        return entity;
    }
}
