package com.crm.registry.application.catalog.usecases.queries;

import com.crm.common.application.annotations.UseCase;
import com.crm.common.application.pages.Page;
import com.crm.common.application.pages.Pageable;
import com.crm.registry.application.catalog.CatalogAccess;
import com.crm.registry.application.catalog.contracts.readers.EquipmentTypeReader;
import com.crm.registry.application.catalog.schemas.EquipmentTypeSummary;
import com.crm.registry.application.catalog.schemas.StatusFilter;
import com.crm.shared.application.Actor;
import lombok.RequiredArgsConstructor;

/** Lista os tipos de equipamento com as características e paginação. Os três papéis consultam. */
@UseCase(transactional = false)
@RequiredArgsConstructor
public class ListEquipmentTypes {

    private final EquipmentTypeReader reader;

    /**
     * @param actor    quem consulta
     * @param status   status listado; nulo vale {@code ACTIVE}
     * @param pageable página pedida
     */
    public record Params(
            Actor actor,
            StatusFilter status,
            Pageable pageable
    ) {
    }

    public Page<EquipmentTypeSummary> execute(Params params) {
        CatalogAccess.requireReader(params.actor());

        return reader.search(StatusFilter.orActive(params.status()), params.pageable());
    }
}
