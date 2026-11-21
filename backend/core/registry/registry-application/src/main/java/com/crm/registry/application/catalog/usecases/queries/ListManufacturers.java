package com.crm.registry.application.catalog.usecases.queries;

import com.crm.common.application.annotations.UseCase;
import com.crm.common.application.pages.Page;
import com.crm.common.application.pages.Pageable;
import com.crm.registry.application.catalog.CatalogAccess;
import com.crm.registry.application.catalog.contracts.readers.ManufacturerReader;
import com.crm.registry.application.catalog.schemas.ManufacturerFilter;
import com.crm.registry.application.catalog.schemas.ManufacturerSummary;
import com.crm.shared.application.Actor;
import lombok.RequiredArgsConstructor;

/** Lista os fabricantes com filtro e paginação. Os três papéis consultam. */
@UseCase(transactional = false)
@RequiredArgsConstructor
public class ListManufacturers {

    private final ManufacturerReader reader;

    /**
     * @param actor    quem consulta
     * @param filter   texto no nome e status; status nulo vale {@code ACTIVE}
     * @param pageable página pedida
     */
    public record Params(
            Actor actor,
            ManufacturerFilter filter,
            Pageable pageable
    ) {
    }

    public Page<ManufacturerSummary> execute(Params params) {
        CatalogAccess.requireReader(params.actor());

        return reader.search(params.filter(), params.pageable());
    }
}
