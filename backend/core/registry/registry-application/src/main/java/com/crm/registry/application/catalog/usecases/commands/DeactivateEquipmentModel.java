package com.crm.registry.application.catalog.usecases.commands;

import com.crm.common.application.annotations.UseCase;
import com.crm.common.application.exceptions.NotFoundException;
import com.crm.registry.application.catalog.CatalogAccess;
import com.crm.registry.application.catalog.contracts.repositories.EquipmentModelRepository;
import com.crm.shared.application.Actor;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

/** Desativa um modelo de equipamento. Administrador e Comercial (ADR-6). */
@UseCase
@RequiredArgsConstructor
public class DeactivateEquipmentModel {

    private final EquipmentModelRepository repository;

    /**
     * @param actor   quem desativa
     * @param modelId modelo desativado
     */
    public record Params(
            Actor actor,
            UUID modelId
    ) {
    }

    public void execute(Params params) {
        CatalogAccess.requireEditor(params.actor());

        var model = repository.findById(params.modelId())
                .orElseThrow(() -> new NotFoundException("model_not_found", "modelo não encontrado"));

        model.deactivate();

        repository.save(model);
    }
}
