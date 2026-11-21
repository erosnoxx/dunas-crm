package com.crm.registry.application.catalog.contracts.readers;

import com.crm.common.application.pages.Page;
import com.crm.common.application.pages.Pageable;
import com.crm.registry.application.catalog.schemas.EquipmentModelFilter;
import com.crm.registry.application.catalog.schemas.EquipmentModelSummary;

import java.util.Optional;
import java.util.UUID;

/** Leitura de modelos de equipamento, direto em projeção, sem montar o aggregate. */
public interface EquipmentModelReader {

    Page<EquipmentModelSummary> search(
            EquipmentModelFilter filter,
            Pageable pageable
    );

    Optional<EquipmentModelSummary> findSummary(UUID id);

    /** Diz se algum modelo (ativo ou não) usa o tipo; é o que trava a característica (ADR-4). */
    boolean hasModelsOfType(UUID typeId);
}
