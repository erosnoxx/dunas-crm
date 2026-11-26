package com.crm.registry.application.catalog.criteria;

import com.crm.common.application.repository.Criteria;
import com.crm.registry.application.catalog.contracts.repositories.EquipmentModelRepository;

/** Filtros de busca de {@link EquipmentModelRepository}; a listagem das telas usa o {@code EquipmentModelReader}. */
public record EquipmentModelCriteria() implements Criteria {
}
