package com.crm.registry.application.customers.usecases.commands;

import com.crm.common.application.annotations.UseCase;
import com.crm.common.application.exceptions.NotFoundException;
import com.crm.registry.application.customers.CustomerAccess;
import com.crm.registry.application.customers.contracts.repositories.CustomerRepository;
import com.crm.shared.application.Actor;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

/** Reativa um cliente desativado. Administrador e Comercial (ADR-7). */
@UseCase
@RequiredArgsConstructor
public class ReactivateCustomer {

    private final CustomerRepository repository;

    /**
     * @param actor      quem reativa
     * @param customerId cliente reativado
     */
    public record Params(
            Actor actor,
            UUID customerId
    ) {
    }

    public void execute(Params params) {
        CustomerAccess.requireEditor(params.actor());

        var customer = repository.findById(params.customerId())
                .orElseThrow(() -> new NotFoundException("customer_not_found", "cliente não encontrado"));

        customer.reactivate();

        repository.save(customer);
    }
}
