package com.crm.registry.application.customers.usecases.commands;

import com.crm.common.application.annotations.UseCase;
import com.crm.common.application.exceptions.NotFoundException;
import com.crm.registry.application.customers.CustomerAccess;
import com.crm.registry.application.customers.contracts.repositories.CustomerRepository;
import com.crm.registry.application.customers.contracts.repositories.LocationRepository;
import com.crm.registry.application.customers.schemas.AddressInput;
import com.crm.registry.domain.customers.entities.Location;
import com.crm.registry.domain.customers.vos.LocationName;
import com.crm.shared.application.Actor;
import com.crm.shared.application.schemas.response.UUIDResponse;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

/**
 * Cadastra um local de um cliente. Administrador e Comercial (ADR-7).
 *
 * <p>Ordem: papel, formato de todos os campos (nome e endereço), cliente existente e, por fim, a
 * regra do aggregate (cliente ativo).
 */
@UseCase
@RequiredArgsConstructor
public class RegisterLocation {

    private final CustomerRepository customers;
    private final LocationRepository locations;

    /**
     * @param actor      quem cadastra
     * @param customerId dono do local
     * @param name       nome do local
     * @param address    endereço
     */
    public record Params(
            Actor actor,
            UUID customerId,
            String name,
            AddressInput address
    ) {
    }

    public UUIDResponse execute(Params params) {
        CustomerAccess.requireEditor(params.actor());

        var name = LocationName.of(params.name());
        var address = AddressParser.parse(params.address());

        var customer = customers.findById(params.customerId())
                .orElseThrow(() -> new NotFoundException("customer_not_found", "cliente não encontrado"));

        var location = Location.register(customer, name, address);

        locations.save(location);

        return new UUIDResponse(location.getId());
    }
}
