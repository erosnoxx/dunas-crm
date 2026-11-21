package com.crm.registry.application.catalog.schemas;

import com.crm.registry.domain.catalog.vos.CatalogStatus;
import com.crm.registry.domain.catalog.vos.MeasurementUnit;

import java.util.List;
import java.util.UUID;

/**
 * Linha da lista de tipos, com as características que o formulário de modelo usa para montar os campos.
 *
 * @param modelCount quantos modelos o tipo tem, ativos ou não
 */
public record EquipmentTypeSummary(
        UUID id,
        String name,
        CatalogStatus status,
        long modelCount,
        List<CharacteristicView> characteristics
) {

    /** Definição de característica do tipo. */
    public record CharacteristicView(
            UUID id,
            String name,
            MeasurementUnit unit,
            boolean required
    ) {
    }
}
