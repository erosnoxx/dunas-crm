package com.crm.registry.application.catalog.schemas;

import com.crm.registry.domain.catalog.enums.CatalogStatus;

import java.util.UUID;

/**
 * Linha da lista de fabricantes.
 *
 * @param modelCount quantos modelos o fabricante tem, ativos ou não
 */
public record ManufacturerSummary(
        UUID id,
        String name,
        CatalogStatus status,
        long modelCount
) {
}
