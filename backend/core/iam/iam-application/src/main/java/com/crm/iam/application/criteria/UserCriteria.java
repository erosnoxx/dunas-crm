package com.crm.iam.application.criteria;

import com.crm.common.application.repository.Criteria;
import com.crm.iam.application.contracts.repositories.UserRepository;

/** Filtros de busca de {@link UserRepository}; a listagem das telas usa o {@code UserReader}. */
public record UserCriteria() implements Criteria {
}
