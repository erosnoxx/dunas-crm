package com.crm.registry.application.customers.usecases.commands;

import com.crm.common.application.annotations.UseCase;
import com.crm.common.application.exceptions.NotFoundException;
import com.crm.registry.application.customers.CustomerAccess;
import com.crm.registry.application.customers.contracts.repositories.LocationRepository;
import com.crm.registry.application.customers.schemas.AddressInput;
import com.crm.registry.domain.customers.vos.LocationName;
import com.crm.shared.application.Actor;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

/**
 * Edita nome e endereço de um local. Administrador e Comercial (ADR-7). O cliente não é lido: o
 * local de um cliente desativado continua editável (ADR-30).
 */
@UseCase
@RequiredArgsConstructor
public class UpdateLocation {

    private final LocationRepository repository;

    /**
     * @param actor      quem edita
     * @param locationId local editado
     * @param name       novo nome
     * @param address    novo endereço
     */
    public record Params(
            Actor actor,
            UUID locationId,
            String name,
            AddressInput address
    ) {
    }

    public void execute(Params params) {
        CustomerAccess.requireEditor(params.actor());

        var name = LocationName.of(params.name());
        var address = AddressParser.parse(params.address());

        var location = repository.findById(params.locationId())
                .orElseThrow(() -> new NotFoundException("location_not_found", "local não encontrado"));

        location.edit(name, address);

        repository.save(location);
    }
}
