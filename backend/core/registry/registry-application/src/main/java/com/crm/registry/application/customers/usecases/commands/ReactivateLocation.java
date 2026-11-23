package com.crm.registry.application.customers.usecases.commands;

import com.crm.common.application.annotations.UseCase;
import com.crm.common.application.exceptions.NotFoundException;
import com.crm.registry.application.customers.CustomerAccess;
import com.crm.registry.application.customers.contracts.repositories.LocationRepository;
import com.crm.shared.application.Actor;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

/** Reativa um local. Administrador e Comercial (ADR-7). O cliente não é lido (ADR-30). */
@UseCase
@RequiredArgsConstructor
public class ReactivateLocation {

    private final LocationRepository repository;

    /**
     * @param actor      quem reativa
     * @param locationId local reativado
     */
    public record Params(
            Actor actor,
            UUID locationId
    ) {
    }

    public void execute(Params params) {
        CustomerAccess.requireEditor(params.actor());

        var location = repository.findById(params.locationId())
                .orElseThrow(() -> new NotFoundException("location_not_found", "local não encontrado"));

        location.reactivate();

        repository.save(location);
    }
}
