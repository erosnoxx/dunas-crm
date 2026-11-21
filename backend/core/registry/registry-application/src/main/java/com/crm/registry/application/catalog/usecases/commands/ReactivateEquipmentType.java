package com.crm.registry.application.catalog.usecases.commands;

import com.crm.common.application.annotations.UseCase;
import com.crm.common.application.exceptions.NotFoundException;
import com.crm.registry.application.catalog.CatalogAccess;
import com.crm.registry.application.catalog.contracts.repositories.EquipmentTypeRepository;
import com.crm.shared.application.Actor;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

/** Reativa um tipo de equipamento. Administrador e Comercial (ADR-6). */
@UseCase
@RequiredArgsConstructor
public class ReactivateEquipmentType {

    private final EquipmentTypeRepository repository;

    /**
     * @param actor  quem reativa
     * @param typeId tipo reativado
     */
    public record Params(
            Actor actor,
            UUID typeId
    ) {
    }

    public void execute(Params params) {
        CatalogAccess.requireEditor(params.actor());

        var type = repository.findById(params.typeId())
                .orElseThrow(() -> new NotFoundException("type_not_found", "tipo não encontrado"));

        type.reactivate();

        repository.save(type);
    }
}
