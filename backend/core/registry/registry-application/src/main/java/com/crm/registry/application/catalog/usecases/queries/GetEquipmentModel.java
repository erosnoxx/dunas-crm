package com.crm.registry.application.catalog.usecases.queries;

import com.crm.common.application.annotations.UseCase;
import com.crm.common.application.exceptions.NotFoundException;
import com.crm.registry.application.catalog.CatalogAccess;
import com.crm.registry.application.catalog.contracts.readers.EquipmentModelReader;
import com.crm.registry.application.catalog.schemas.EquipmentModelSummary;
import com.crm.shared.application.Actor;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

/** Detalhe de um modelo. Os três papéis consultam. */
@UseCase(transactional = false)
@RequiredArgsConstructor
public class GetEquipmentModel {

    private final EquipmentModelReader reader;

    /**
     * @param actor   quem consulta
     * @param modelId modelo consultado
     */
    public record Params(
            Actor actor,
            UUID modelId
    ) {
    }

    public EquipmentModelSummary execute(Params params) {
        CatalogAccess.requireReader(params.actor());

        return reader.findSummary(params.modelId())
                .orElseThrow(() -> new NotFoundException("model_not_found", "modelo não encontrado"));
    }
}
