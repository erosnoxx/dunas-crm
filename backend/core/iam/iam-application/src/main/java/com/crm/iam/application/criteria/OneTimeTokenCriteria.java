package com.crm.iam.application.criteria;

import com.crm.common.application.repository.Criteria;
import com.crm.iam.application.contracts.repositories.OneTimeTokenRepository;

/** Filtros de busca de {@link OneTimeTokenRepository}; as consultas do IAM têm método próprio. */
public record OneTimeTokenCriteria() implements Criteria {
}
