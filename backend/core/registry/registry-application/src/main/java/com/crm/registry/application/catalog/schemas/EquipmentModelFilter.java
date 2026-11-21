package com.crm.registry.application.catalog.schemas;

import java.util.UUID;

/**
 * Filtros da lista de modelos; campo nulo ou vazio não filtra e status nulo vale {@code ACTIVE}.
 *
 * @param text   trecho do nome do modelo ou do fabricante
 * @param typeId tipo dos modelos
 */
public record EquipmentModelFilter(
        String text,
        UUID typeId,
        StatusFilter status
) {

    public EquipmentModelFilter {
        status = StatusFilter.orActive(status);
    }
}
