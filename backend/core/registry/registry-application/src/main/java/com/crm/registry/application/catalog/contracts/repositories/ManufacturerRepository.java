package com.crm.registry.application.catalog.contracts.repositories;

import com.crm.common.application.repository.Repository;
import com.crm.registry.domain.catalog.entities.Manufacturer;
import com.crm.registry.domain.catalog.vos.ManufacturerName;

import java.util.UUID;

/** Persistência do aggregate {@link Manufacturer}. */
public interface ManufacturerRepository extends Repository<Manufacturer, ManufacturerCriteria> {

    /**
     * Diz se já existe um fabricante com este nome, sem diferenciar maiúsculas.
     *
     * @param exceptId fabricante ignorado na comparação (o próprio, ao renomear); nulo = nenhum
     */
    boolean existsByName(ManufacturerName name, UUID exceptId);
}
