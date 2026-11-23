package com.crm.registry.application.customers.contracts.repositories;

import com.crm.common.application.repository.Repository;
import com.crm.registry.application.customers.criteria.InstalledEquipmentCriteria;
import com.crm.registry.domain.customers.entities.InstalledEquipment;
import com.crm.registry.domain.customers.vos.SerialNumber;

import java.util.UUID;

/** Persistência da entidade {@link InstalledEquipment}. */
public interface InstalledEquipmentRepository extends Repository<InstalledEquipment, InstalledEquipmentCriteria> {

    /**
     * Diz se o modelo já tem um equipamento instalado com este número de série, sem diferenciar maiúsculas.
     *
     * @param exceptId equipamento ignorado na comparação (o próprio, ao editar); nulo = nenhum
     */
    boolean existsByModelAndSerialNumber(
            UUID modelId,
            SerialNumber serialNumber,
            UUID exceptId
    );
}
