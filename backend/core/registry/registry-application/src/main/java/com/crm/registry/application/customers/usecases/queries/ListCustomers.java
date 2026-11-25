package com.crm.registry.application.customers.usecases.queries;

import com.crm.common.application.annotations.UseCase;
import com.crm.common.application.pages.Page;
import com.crm.common.application.pages.Pageable;
import com.crm.registry.application.customers.CustomerAccess;
import com.crm.registry.application.customers.contracts.readers.CustomerReader;
import com.crm.registry.application.customers.schemas.CustomerFilter;
import com.crm.registry.application.customers.schemas.CustomerSummary;
import com.crm.shared.application.Actor;
import lombok.RequiredArgsConstructor;

/** Lista os clientes com filtro e paginação. Os três papéis consultam. */
@UseCase(transactional = false)
@RequiredArgsConstructor
public class ListCustomers {

    private final CustomerReader reader;

    /**
     * @param actor    quem consulta
     * @param filter   texto e status; status nulo vale {@code ACTIVE}
     * @param pageable página pedida
     */
    public record Params(
            Actor actor,
            CustomerFilter filter,
            Pageable pageable
    ) {
    }

    public Page<CustomerSummary> execute(Params params) {
        CustomerAccess.requireReader(params.actor());

        return reader.search(params.filter(), params.pageable());
    }
}
