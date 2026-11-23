package com.crm.registry.application.customers;

import com.crm.common.application.exceptions.ApplicationException;
import com.crm.common.domain.exceptions.DomainException;
import com.crm.registry.domain.customers.entities.Customer;
import com.crm.registry.domain.customers.vos.CustomerName;
import com.crm.shared.application.Actor;
import com.crm.shared.domain.enums.Role;
import com.crm.shared.domain.vos.contact.Email;
import com.crm.shared.domain.vos.identity.Document;
import org.assertj.core.api.ThrowingConsumer;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** Dados de teste compartilhados pelos testes dos use cases de clientes. */
public final class CustomersFixtures {

    public static final Actor ADMINISTRATOR = actor(Role.ADMINISTRATOR);
    public static final Actor COMMERCIAL = actor(Role.COMMERCIAL);
    public static final Actor TECHNICIAN = actor(Role.TECHNICIAN);

    public static final String CPF = "529.982.247-25";
    public static final String CNPJ = "12.ABC.345/01DE-35";

    private CustomersFixtures() {
    }

    public static Actor actor(Role role) {
        return new Actor(UUID.randomUUID(), role, UUID.randomUUID());
    }

    /** Pessoa física ativa com e-mail. */
    public static Customer individual() {
        return Customer.register(Document.of(CPF), CustomerName.of("Maria Souza"), null, Email.of("maria@x.com"), null);
    }

    public static ThrowingConsumer<Throwable> applicationCode(String code) {
        return e -> assertThat(((ApplicationException) e).getCode()).contains(code);
    }

    /** Também serve a ValidationException, que é um DomainException com código. */
    public static ThrowingConsumer<Throwable> domainCode(String code) {
        return e -> assertThat(((DomainException) e).getCode()).contains(code);
    }
}
