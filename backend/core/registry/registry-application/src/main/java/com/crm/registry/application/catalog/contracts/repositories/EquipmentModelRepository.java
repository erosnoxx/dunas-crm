package com.crm.registry.application.catalog.contracts.repositories;

import com.crm.common.application.repository.Repository;
import com.crm.registry.application.catalog.criteria.EquipmentModelCriteria;
import com.crm.registry.domain.catalog.entities.EquipmentModel;
import com.crm.registry.domain.catalog.vos.ModelName;

import java.util.UUID;

/** Persistência do aggregate {@link EquipmentModel}. */
public interface EquipmentModelRepository extends Repository<EquipmentModel, EquipmentModelCriteria> {

    /**
     * Diz se o fabricante já tem um modelo com este nome, sem diferenciar maiúsculas.
     *
     * @param exceptId modelo ignorado na comparação (o próprio, ao renomear); nulo = nenhum
     */
    boolean existsByManufacturerAndName(UUID manufacturerId, ModelName name, UUID exceptId);
}
