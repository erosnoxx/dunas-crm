package com.crm.registry.application.customers.criteria;

import com.crm.common.application.repository.Criteria;
import com.crm.registry.application.customers.contracts.repositories.CustomerRepository;

/** Filtros de busca de {@link CustomerRepository}; a listagem das telas usa um reader. */
public record CustomerCriteria() implements Criteria {
}
