package com.crm.registry.application.catalog.usecases.commands;

import com.crm.common.application.annotations.UseCase;
import com.crm.common.application.exceptions.ConflictException;
import com.crm.common.application.exceptions.NotFoundException;
import com.crm.registry.application.catalog.CatalogAccess;
import com.crm.registry.application.catalog.contracts.repositories.ManufacturerRepository;
import com.crm.registry.domain.catalog.vos.ManufacturerName;
import com.crm.shared.application.Actor;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

/**
 * Renomeia um fabricante. Administrador e Comercial (ADR-6).
 *
 * <p>O próprio fabricante fica fora da checagem de nome, então renomear para o mesmo nome (ou só
 * trocar a caixa) não conflita consigo mesmo.
 */
@UseCase
@RequiredArgsConstructor
public class RenameManufacturer {

    private final ManufacturerRepository repository;

    /**
     * @param actor          quem renomeia
     * @param manufacturerId fabricante renomeado
     * @param name           novo nome
     */
    public record Params(
            Actor actor,
            UUID manufacturerId,
            String name
    ) {
    }

    public void execute(Params params) {
        CatalogAccess.requireEditor(params.actor());

        var manufacturer = repository.findById(params.manufacturerId())
                .orElseThrow(() -> new NotFoundException("manufacturer_not_found", "fabricante não encontrado"));

        var name = ManufacturerName.of(params.name());

        if (repository.existsByName(name, manufacturer.getId()))
            throw new ConflictException("manufacturer_name_taken", "já existe um fabricante com este nome");

        manufacturer.rename(name);

        repository.save(manufacturer);
    }
}
