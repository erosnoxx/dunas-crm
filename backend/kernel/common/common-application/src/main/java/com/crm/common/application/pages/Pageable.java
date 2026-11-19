package com.crm.common.application.pages;

/**
 * Pedido de paginação, independente de framework.
 *
 * @param page          índice da página, começando em 0
 * @param size          itens por página
 * @param sortBy        campo de ordenação, ou {@code null} para não ordenar
 * @param sortDirection {@code "ASC"} ou {@code "DESC"}; qualquer outro valor vale {@code ASC}
 */
public record Pageable(
        int page,
        int size,
        String sortBy,
        String sortDirection
) {
    public static Pageable of(int page, int size) {
        return new Pageable(page, size, null, null);
    }

    public static Pageable of(int page, int size, String sortBy, String sortDirection) {
        return new Pageable(page, size, sortBy, sortDirection);
    }

    public int getOffset() {
        return page * size;
    }

    public int getPageSize() {
        return size;
    }
}
