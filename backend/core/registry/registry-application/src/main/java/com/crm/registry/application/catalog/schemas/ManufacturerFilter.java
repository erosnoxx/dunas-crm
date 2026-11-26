package com.crm.registry.application.catalog.schemas;

/**
 * Filtros da lista de fabricantes; texto nulo ou vazio não filtra e status nulo vale {@code ACTIVE}.
 *
 * @param text trecho do nome
 */
public record ManufacturerFilter(
        String text,
        StatusFilter status
) {

    public ManufacturerFilter {
        status = StatusFilter.orActive(status);
    }
}
