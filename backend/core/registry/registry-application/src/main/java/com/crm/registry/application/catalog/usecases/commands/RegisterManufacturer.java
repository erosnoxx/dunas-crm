package com.crm.registry.application.catalog.usecases.commands;

import com.crm.common.application.annotations.UseCase;
import com.crm.common.application.exceptions.ConflictException;
import com.crm.registry.application.catalog.CatalogAccess;
import com.crm.registry.application.catalog.contracts.repositories.ManufacturerRepository;
import com.crm.registry.domain.catalog.entities.Manufacturer;
import com.crm.registry.domain.catalog.vos.ManufacturerName;
import com.crm.shared.application.Actor;
import com.crm.shared.application.schemas.response.UUIDResponse;
import lombok.RequiredArgsConstructor;

/**
 * Cadastra um fabricante. Administrador e Comercial (ADR-6).
 *
 * <p>A checagem de nome repetido é só a resposta amigável; quem garante é o UNIQUE do banco.
 */
@UseCase
@RequiredArgsConstructor
public class RegisterManufacturer {

    private final ManufacturerRepository repository;

    /**
     * @param actor quem cadastra
     * @param name  nome do fabricante
     */
    public record Params(
            Actor actor,
            String name
    ) {
    }

    public UUIDResponse execute(Params params) {
        CatalogAccess.requireEditor(params.actor());

        var name = ManufacturerName.of(params.name());

        if (repository.existsByName(name, null))
            throw new ConflictException("manufacturer_name_taken", "já existe um fabricante com este nome");

        var manufacturer = Manufacturer.register(name);

        repository.save(manufacturer);

        return new UUIDResponse(manufacturer.getId());
    }
}
