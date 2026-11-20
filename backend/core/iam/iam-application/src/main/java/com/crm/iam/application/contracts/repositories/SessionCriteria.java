package com.crm.iam.application.contracts.repositories;

import com.crm.common.application.repository.Criteria;

/** Filtros de busca de {@link SessionRepository}; as consultas do IAM têm método próprio. */
public record SessionCriteria() implements Criteria {
}
