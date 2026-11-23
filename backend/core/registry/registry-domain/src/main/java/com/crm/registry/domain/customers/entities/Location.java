package com.crm.registry.domain.customers.entities;

import com.crm.common.domain.entities.AggregateRoot;
import com.crm.common.domain.exceptions.DomainException;
import com.crm.registry.domain.customers.enums.CustomerStatus;
import com.crm.registry.domain.customers.vos.LocationName;
import com.crm.shared.domain.vos.address.Address;

import java.util.UUID;

/**
 * Local de um cliente (onde ficam as usinas).
 *
 * <p>Aggregate próprio: guarda só o {@code customerId} (imutável). Um cliente desativado bloqueia
 * apenas locais NOVOS; editar, desativar e reativar um local não recebem o cliente (ADR-6, ADR-30).
 * O ciclo de vida é um enum com guardas; nada se apaga e nenhuma mudança gera domain event.
 */
public class Location extends AggregateRoot {

    private final UUID customerId;

    private LocationName name;

    private Address address;

    private CustomerStatus status;

    private Location(
            UUID id,
            Long version,
            UUID customerId,
            LocationName name,
            Address address,
            CustomerStatus status
    ) {
        super(id, version);

        this.customerId = customerId;
        this.name = name;
        this.address = address;
        this.status = status;
    }

    /** Cadastra o local, que nasce ativo; só vale para um cliente ativo. */
    public static Location register(
            Customer customer,
            LocationName name,
            Address address
    ) {
        if (!customer.isActive())
            throw new DomainException("customer_deactivated", "este cliente está desativado");

        return new Location(
                UUID.randomUUID(),
                null,
                customer.getId(),
                name,
                address,
                CustomerStatus.ACTIVE
        );
    }

    /** Reconstrução do banco: não valida de novo, não registra evento. */
    public static Location reconstitute(
            UUID id,
            Long version,
            UUID customerId,
            LocationName name,
            Address address,
            CustomerStatus status
    ) {
        return new Location(
                id,
                version,
                customerId,
                name,
                address,
                status
        );
    }

    public UUID getCustomerId() {
        return customerId;
    }

    public LocationName getName() {
        return name;
    }

    public Address getAddress() {
        return address;
    }

    public CustomerStatus getStatus() {
        return status;
    }

    public boolean isActive() {
        return status == CustomerStatus.ACTIVE;
    }

    /** Substitui nome e endereço. */
    public void edit(LocationName name, Address address) {
        requireActive();

        this.name = name;
        this.address = address;
    }

    public void deactivate() {
        requireActive();

        this.status = CustomerStatus.DEACTIVATED;
    }

    public void reactivate() {
        if (status != CustomerStatus.DEACTIVATED)
            throw new DomainException("location_not_deactivated", "este local não está desativado");

        this.status = CustomerStatus.ACTIVE;
    }

    private void requireActive() {
        if (status != CustomerStatus.ACTIVE)
            throw new DomainException("location_deactivated", "este local está desativado");
    }
}
