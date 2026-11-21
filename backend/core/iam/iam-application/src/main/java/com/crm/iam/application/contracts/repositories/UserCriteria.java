package com.crm.iam.application.contracts.repositories;

import com.crm.common.application.repository.Criteria;

/** Filtros de busca de {@link UserRepository}; a listagem das telas usa o {@code UserReader}. */
public record UserCriteria() implements Criteria {
}
