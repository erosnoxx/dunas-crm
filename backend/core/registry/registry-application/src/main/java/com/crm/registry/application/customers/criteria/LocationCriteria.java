package com.crm.registry.application.customers.criteria;

import com.crm.common.application.repository.Criteria;
import com.crm.registry.application.customers.contracts.repositories.LocationRepository;

/** Filtros de busca de {@link LocationRepository}; a listagem das telas usa um reader. */
public record LocationCriteria() implements Criteria {
}
