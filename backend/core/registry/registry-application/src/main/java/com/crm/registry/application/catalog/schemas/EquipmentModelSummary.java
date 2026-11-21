package com.crm.registry.application.catalog.schemas;

import com.crm.registry.domain.catalog.vos.CatalogStatus;
import com.crm.registry.domain.catalog.vos.MeasurementUnit;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Linha da lista de modelos e detalhe de um modelo.
 *
 * @param manufacturer fabricante do modelo
 * @param type         tipo do modelo
 * @param values       valores informados, com nome e unidade da característica
 */
public record EquipmentModelSummary(
        UUID id,
        String name,
        CatalogStatus status,
        Ref manufacturer,
        Ref type,
        List<ValueView> values
) {

    /** Referência por id e nome a fabricante ou tipo. */
    public record Ref(
            UUID id,
            String name
    ) {
    }

    /** Valor de uma característica, já com o nome e a unidade dela. */
    public record ValueView(
            UUID definitionId,
            String name,
            MeasurementUnit unit,
            BigDecimal amount
    ) {
    }
}
