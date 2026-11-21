package com.crm.registry.application.catalog.contracts.readers;

import com.crm.common.application.pages.Page;
import com.crm.common.application.pages.Pageable;
import com.crm.registry.application.catalog.schemas.EquipmentTypeSummary;
import com.crm.registry.application.catalog.schemas.StatusFilter;

/** Leitura de tipos de equipamento para as telas, direto em projeção, sem montar o aggregate. */
public interface EquipmentTypeReader {

    Page<EquipmentTypeSummary> search(
            StatusFilter status,
            Pageable pageable
    );
}
