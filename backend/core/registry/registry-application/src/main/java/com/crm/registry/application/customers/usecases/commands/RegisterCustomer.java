package com.crm.registry.application.customers.usecases.commands;

import com.crm.common.application.annotations.UseCase;
import com.crm.common.application.exceptions.ConflictException;
import com.crm.registry.application.customers.CustomerAccess;
import com.crm.registry.application.customers.contracts.repositories.CustomerRepository;
import com.crm.registry.domain.customers.entities.Customer;
import com.crm.registry.domain.customers.vos.CustomerName;
import com.crm.shared.application.Actor;
import com.crm.shared.application.schemas.response.UUIDResponse;
import com.crm.shared.domain.vos.contact.Email;
import com.crm.shared.domain.vos.contact.Phone;
import com.crm.shared.domain.vos.identity.Document;
import lombok.RequiredArgsConstructor;

/**
 * Cadastra um cliente. Administrador e Comercial (ADR-7).
 *
 * <p>Ordem: papel, formato de todos os campos, regras do aggregate e só então a unicidade do
 * documento. A checagem é a resposta amigável; quem garante é o UNIQUE do banco.
 */
@UseCase
@RequiredArgsConstructor
public class RegisterCustomer {

    private final CustomerRepository repository;

    /**
     * @param actor     quem cadastra
     * @param document  CPF ou CNPJ
     * @param name      nome ou razão social
     * @param tradeName nome fantasia (só pessoa jurídica); nulo = ausente
     * @param email     e-mail; nulo = ausente
     * @param phone     telefone; nulo = ausente
     */
    public record Params(
            Actor actor,
            String document,
            String name,
            String tradeName,
            String email,
            String phone
    ) {
    }

    public UUIDResponse execute(Params params) {
        CustomerAccess.requireEditor(params.actor());

        var document = Document.of(params.document());
        var name = CustomerName.of(params.name());
        var tradeName = OptionalValue.of(params.tradeName(), CustomerName::of);
        var email = OptionalValue.of(params.email(), Email::of);
        var phone = OptionalValue.of(params.phone(), Phone::of);

        var customer = Customer.register(
                document,
                name,
                tradeName,
                email,
                phone
        );

        if (repository.existsByDocument(document))
            throw new ConflictException("customer_document_taken", "já existe um cliente com este documento");

        repository.save(customer);

        return new UUIDResponse(customer.getId());
    }
}
