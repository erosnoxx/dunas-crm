package com.crm.registry.domain.customers.entities;

import com.crm.common.domain.entities.AggregateRoot;
import com.crm.common.domain.exceptions.DomainException;
import com.crm.registry.domain.customers.enums.CustomerStatus;
import com.crm.registry.domain.customers.vos.CustomerName;
import com.crm.shared.domain.vos.contact.Email;
import com.crm.shared.domain.vos.contact.Phone;
import com.crm.shared.domain.vos.identity.Document;

import java.util.Optional;
import java.util.UUID;

/**
 * Cliente da empresa (pessoa física ou jurídica, conforme o documento).
 *
 * <p>O documento é definido no cadastro e nunca muda: não há setter nem método de edição para ele.
 * É obrigatório ao menos um contato (e-mail ou telefone), e nome fantasia só vale para pessoa
 * jurídica. O ciclo de vida é um enum com guardas (ativo, desativado); só um cliente ativo é
 * editado ou desativado. Nada se apaga e nenhuma mudança gera domain event.
 *
 * <p>As mensagens de erro nunca carregam documento, e-mail ou telefone.
 */
public class Customer extends AggregateRoot {

    private final Document document;

    private CustomerName name;

    private CustomerName tradeName;

    private Email email;

    private Phone phone;

    private CustomerStatus status;

    private Customer(
            UUID id,
            Long version,
            Document document,
            CustomerName name,
            CustomerName tradeName,
            Email email,
            Phone phone,
            CustomerStatus status
    ) {
        super(id, version);

        this.document = document;
        this.name = name;
        this.tradeName = tradeName;
        this.email = email;
        this.phone = phone;
        this.status = status;
    }

    /** Cadastra o cliente, que nasce ativo. Nome fantasia, e-mail e telefone podem ser nulos. */
    public static Customer register(
            Document document,
            CustomerName name,
            CustomerName tradeName,
            Email email,
            Phone phone
    ) {
        requireContact(email, phone);
        requireTradeNameAllowed(document, tradeName);

        return new Customer(UUID.randomUUID(), null, document, name, tradeName, email, phone, CustomerStatus.ACTIVE);
    }

    /** Reconstrução do banco: não valida de novo, não registra evento. */
    public static Customer reconstitute(
            UUID id,
            Long version,
            Document document,
            CustomerName name,
            CustomerName tradeName,
            Email email,
            Phone phone,
            CustomerStatus status
    ) {
        return new Customer(id, version, document, name, tradeName, email, phone, status);
    }

    public Document getDocument() {
        return document;
    }

    public CustomerName getName() {
        return name;
    }

    public Optional<CustomerName> getTradeName() {
        return Optional.ofNullable(tradeName);
    }

    public Optional<Email> getEmail() {
        return Optional.ofNullable(email);
    }

    public Optional<Phone> getPhone() {
        return Optional.ofNullable(phone);
    }

    public CustomerStatus getStatus() {
        return status;
    }

    public boolean isActive() {
        return status == CustomerStatus.ACTIVE;
    }

    /** Substitui nome, nome fantasia e contatos (nulo remove); o documento nunca muda. */
    public void edit(CustomerName name, CustomerName tradeName, Email email, Phone phone) {
        requireActive();
        requireContact(email, phone);
        requireTradeNameAllowed(document, tradeName);

        this.name = name;
        this.tradeName = tradeName;
        this.email = email;
        this.phone = phone;
    }

    public void deactivate() {
        requireActive();

        this.status = CustomerStatus.DEACTIVATED;
    }

    public void reactivate() {
        if (status != CustomerStatus.DEACTIVATED)
            throw new DomainException("customer_not_deactivated", "este cliente não está desativado");

        this.status = CustomerStatus.ACTIVE;
    }

    private void requireActive() {
        if (status != CustomerStatus.ACTIVE)
            throw new DomainException("customer_deactivated", "este cliente está desativado");
    }

    private static void requireContact(Email email, Phone phone) {
        if (email == null && phone == null)
            throw new DomainException("contact_required", "informe pelo menos um contato: e-mail ou telefone");
    }

    private static void requireTradeNameAllowed(Document document, CustomerName tradeName) {
        if (tradeName != null && document.isIndividual())
            throw new DomainException("trade_name_not_allowed", "nome fantasia só vale para pessoa jurídica");
    }
}
