package com.crm.registry.application.customers.usecases.commands;

import com.crm.common.application.annotations.UseCase;
import com.crm.common.application.exceptions.NotFoundException;
import com.crm.registry.application.customers.CustomerAccess;
import com.crm.registry.application.customers.contracts.repositories.CustomerRepository;
import com.crm.registry.domain.customers.vos.CustomerName;
import com.crm.shared.application.Actor;
import com.crm.shared.domain.vos.contact.Email;
import com.crm.shared.domain.vos.contact.Phone;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

/**
 * Edita nome, nome fantasia e contatos de um cliente; o documento nunca muda. Administrador e
 * Comercial (ADR-7). Os campos são substituídos: nulo remove nome fantasia, e-mail ou telefone.
 */
@UseCase
@RequiredArgsConstructor
public class UpdateCustomer {

    private final CustomerRepository repository;

    /**
     * @param actor      quem edita
     * @param customerId cliente editado
     * @param name       novo nome ou razão social
     * @param tradeName  novo nome fantasia; nulo remove
     * @param email      novo e-mail; nulo remove
     * @param phone      novo telefone; nulo remove
     */
    public record Params(
            Actor actor,
            UUID customerId,
            String name,
            String tradeName,
            String email,
            String phone
    ) {
    }

    public void execute(Params params) {
        CustomerAccess.requireEditor(params.actor());

        var name = CustomerName.of(params.name());
        var tradeName = OptionalValue.of(params.tradeName(), CustomerName::of);
        var email = OptionalValue.of(params.email(), Email::of);
        var phone = OptionalValue.of(params.phone(), Phone::of);

        var customer = repository.findById(params.customerId())
                .orElseThrow(() -> new NotFoundException("customer_not_found", "cliente não encontrado"));

        customer.edit(name, tradeName, email, phone);

        repository.save(customer);
    }
}
