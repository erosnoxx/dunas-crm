package com.crm.registry.application.catalog.criteria;

import com.crm.common.application.repository.Criteria;
import com.crm.registry.application.catalog.contracts.repositories.ManufacturerRepository;

/** Filtros de busca de {@link ManufacturerRepository}; a listagem das telas usa o {@code ManufacturerReader}. */
public record ManufacturerCriteria() implements Criteria {
}
