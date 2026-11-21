package com.crm.registry.application.catalog.contracts.readers;

import com.crm.common.application.pages.Page;
import com.crm.common.application.pages.Pageable;
import com.crm.registry.application.catalog.schemas.ManufacturerFilter;
import com.crm.registry.application.catalog.schemas.ManufacturerSummary;

/** Leitura de fabricantes para as telas, direto em projeção, sem montar o aggregate. */
public interface ManufacturerReader {

    Page<ManufacturerSummary> search(
            ManufacturerFilter filter,
            Pageable pageable
    );
}
