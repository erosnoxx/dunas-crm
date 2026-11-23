package com.crm.registry.application.catalog.criteria;

import com.crm.common.application.repository.Criteria;
import com.crm.registry.application.catalog.contracts.repositories.EquipmentTypeRepository;

/** Filtros de busca de {@link EquipmentTypeRepository}; a listagem das telas usa o {@code EquipmentTypeReader}. */
public record EquipmentTypeCriteria() implements Criteria {
}
