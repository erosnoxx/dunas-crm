package com.crm.iam.application.criteria;

import com.crm.common.application.repository.Criteria;
import com.crm.iam.application.contracts.repositories.SessionRepository;

/** Filtros de busca de {@link SessionRepository}; as consultas do IAM têm método próprio. */
public record SessionCriteria() implements Criteria {
}
