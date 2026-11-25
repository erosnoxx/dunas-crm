package com.crm.registry.application.customers;

import com.crm.common.application.exceptions.ApplicationException;
import com.crm.common.domain.exceptions.DomainException;
import com.crm.registry.application.customers.schemas.AddressInput;
import com.crm.registry.domain.customers.entities.Customer;
import com.crm.registry.domain.customers.entities.Location;
import com.crm.registry.domain.customers.vos.LocationName;
import com.crm.shared.domain.enums.UF;
import com.crm.shared.domain.vos.address.Address;
import com.crm.shared.domain.vos.address.AddressNumber;
import com.crm.shared.domain.vos.address.CityName;
import com.crm.shared.domain.vos.address.Neighborhood;
import com.crm.shared.domain.vos.address.Street;
import com.crm.shared.domain.vos.address.ZipCode;
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

    /** Local ativo do cliente, em Recife, sem complemento. */
    public static Location location(Customer customer) {
        return Location.register(customer, LocationName.of("Casa"), new Address(
                ZipCode.of("50030-230"), Street.of("Rua A"), AddressNumber.of("1"), null,
                Neighborhood.of("Centro"), CityName.of("Recife"), UF.PE));
    }

    /** Endereço válido como chega da requisição, sem complemento. */
    public static AddressInput addressInput() {
        return new AddressInput("01310-100", "Avenida Paulista", "1578", null, "Bela Vista", "São Paulo", "SP");
    }

    public static ThrowingConsumer<Throwable> applicationCode(String code) {
        return e -> assertThat(((ApplicationException) e).getCode()).contains(code);
    }

    /** Também serve a ValidationException, que é um DomainException com código. */
    public static ThrowingConsumer<Throwable> domainCode(String code) {
        return e -> assertThat(((DomainException) e).getCode()).contains(code);
    }
}
