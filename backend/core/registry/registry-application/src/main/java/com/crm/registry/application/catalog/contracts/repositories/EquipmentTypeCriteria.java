package com.crm.registry.application.catalog.contracts.repositories;

import com.crm.common.application.repository.Criteria;

/** Filtros de busca de {@link EquipmentTypeRepository}; a listagem das telas usa o {@code EquipmentTypeReader}. */
public record EquipmentTypeCriteria() implements Criteria {
}
