package com.crm.registry.application.catalog.usecases.commands;

import com.crm.common.application.annotations.UseCase;
import com.crm.common.application.exceptions.NotFoundException;
import com.crm.registry.application.catalog.CatalogAccess;
import com.crm.registry.application.catalog.contracts.repositories.ManufacturerRepository;
import com.crm.shared.application.Actor;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

/** Reativa um fabricante desativado. Administrador e Comercial (ADR-6). */
@UseCase
@RequiredArgsConstructor
public class ReactivateManufacturer {

    private final ManufacturerRepository repository;

    /**
     * @param actor          quem reativa
     * @param manufacturerId fabricante reativado
     */
    public record Params(
            Actor actor,
            UUID manufacturerId
    ) {
    }

    public void execute(Params params) {
        CatalogAccess.requireEditor(params.actor());

        var manufacturer = repository.findById(params.manufacturerId())
                .orElseThrow(() -> new NotFoundException("manufacturer_not_found", "fabricante não encontrado"));

        manufacturer.reactivate();

        repository.save(manufacturer);
    }
}
