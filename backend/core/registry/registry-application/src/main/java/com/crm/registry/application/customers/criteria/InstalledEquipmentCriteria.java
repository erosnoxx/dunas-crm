package com.crm.registry.application.customers.criteria;

import com.crm.common.application.repository.Criteria;
import com.crm.registry.application.customers.contracts.repositories.InstalledEquipmentRepository;

/** Filtros de busca de {@link InstalledEquipmentRepository}; a listagem das telas usa um reader. */
public record InstalledEquipmentCriteria() implements Criteria {
}
