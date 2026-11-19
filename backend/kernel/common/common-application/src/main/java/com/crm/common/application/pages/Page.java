package com.crm.common.application.pages;

import java.util.Iterator;
import java.util.List;
import java.util.function.Function;

/**
 * Uma página de resultados, independente de framework.
 *
 * @param content       itens da página
 * @param page          índice da página, começando em 0
 * @param size          tamanho pedido da página
 * @param totalElements total de itens em todas as páginas
 * @param totalPages    total de páginas
 */
public record Page<T>(
        List<T> content,
        int page, int size, long totalElements, int totalPages
) implements Iterable<T> {
    public <R> Page<R> map(Function<T, R> mapper) {
        return new Page<>(
                content.stream().map(mapper).toList(),
                page, size, totalElements, totalPages
        );
    }

    @Override
    public Iterator<T> iterator() {
        return content.iterator();
    }
}
