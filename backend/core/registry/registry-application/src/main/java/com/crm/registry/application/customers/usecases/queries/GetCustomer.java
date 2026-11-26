package com.crm.registry.application.customers.usecases.queries;

import com.crm.common.application.annotations.UseCase;
import com.crm.common.application.exceptions.NotFoundException;
import com.crm.registry.application.customers.CustomerAccess;
import com.crm.registry.application.customers.contracts.readers.CustomerReader;
import com.crm.registry.application.customers.schemas.CustomerDetail;
import com.crm.shared.application.Actor;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

/** Detalhe de um cliente. Os três papéis consultam. */
@UseCase(transactional = false)
@RequiredArgsConstructor
public class GetCustomer {

    private final CustomerReader reader;

    /**
     * @param actor      quem consulta
     * @param customerId cliente consultado
     */
    public record Params(
            Actor actor,
            UUID customerId
    ) {
    }

    public CustomerDetail execute(Params params) {
        CustomerAccess.requireReader(params.actor());

        return reader.findDetail(params.customerId())
                .orElseThrow(() -> new NotFoundException("customer_not_found", "cliente não encontrado"));
    }
}
