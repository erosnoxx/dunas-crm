package com.crm.iam.application.contracts.repositories;

import com.crm.common.application.repository.Criteria;

/** Filtros de busca de {@link OneTimeTokenRepository}; as consultas do IAM têm método próprio. */
public record OneTimeTokenCriteria() implements Criteria {
}
