package com.crm.registry.application.catalog.usecases.queries;

import com.crm.common.application.annotations.UseCase;
import com.crm.common.application.pages.Page;
import com.crm.common.application.pages.Pageable;
import com.crm.registry.application.catalog.CatalogAccess;
import com.crm.registry.application.catalog.contracts.readers.EquipmentModelReader;
import com.crm.registry.application.catalog.schemas.EquipmentModelFilter;
import com.crm.registry.application.catalog.schemas.EquipmentModelSummary;
import com.crm.shared.application.Actor;
import lombok.RequiredArgsConstructor;

/** Lista os modelos com filtro e paginação. Os três papéis consultam. */
@UseCase(transactional = false)
@RequiredArgsConstructor
public class ListEquipmentModels {

    private final EquipmentModelReader reader;

    /**
     * @param actor    quem consulta
     * @param filter   texto, tipo e status; status nulo vale {@code ACTIVE}
     * @param pageable página pedida
     */
    public record Params(
            Actor actor,
            EquipmentModelFilter filter,
            Pageable pageable
    ) {
    }

    public Page<EquipmentModelSummary> execute(Params params) {
        CatalogAccess.requireReader(params.actor());

        return reader.search(params.filter(), params.pageable());
    }
}
