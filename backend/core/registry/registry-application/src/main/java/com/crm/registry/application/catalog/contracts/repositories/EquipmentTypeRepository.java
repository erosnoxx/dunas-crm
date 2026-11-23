package com.crm.registry.application.catalog.contracts.repositories;

import com.crm.common.application.repository.Repository;
import com.crm.registry.application.catalog.criteria.EquipmentTypeCriteria;
import com.crm.registry.domain.catalog.entities.EquipmentType;
import com.crm.registry.domain.catalog.vos.TypeName;

import java.util.UUID;

/** Persistência do aggregate {@link EquipmentType}. */
public interface EquipmentTypeRepository extends Repository<EquipmentType, EquipmentTypeCriteria> {

    /**
     * Diz se já existe um tipo com este nome, sem diferenciar maiúsculas.
     *
     * @param exceptId tipo ignorado na comparação (o próprio, ao renomear); nulo = nenhum
     */
    boolean existsByName(TypeName name, UUID exceptId);
}
