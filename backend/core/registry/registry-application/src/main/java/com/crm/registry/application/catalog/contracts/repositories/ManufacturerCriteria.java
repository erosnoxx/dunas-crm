package com.crm.registry.application.catalog.contracts.repositories;

import com.crm.common.application.repository.Criteria;

/** Filtros de busca de {@link ManufacturerRepository}; a listagem das telas usa o {@code ManufacturerReader}. */
public record ManufacturerCriteria() implements Criteria {
}
