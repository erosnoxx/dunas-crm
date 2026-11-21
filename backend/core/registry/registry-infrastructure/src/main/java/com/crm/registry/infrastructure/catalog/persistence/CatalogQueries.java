package com.crm.registry.infrastructure.catalog.persistence;

import com.crm.common.application.pages.Page;
import com.crm.common.application.pages.Pageable;
import com.crm.registry.application.catalog.schemas.StatusFilter;
import com.crm.registry.domain.catalog.vos.CatalogStatus;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Peças de JPQL que as três listas do catálogo repetem: filtro de situação, texto do LIKE,
 * ordenação e montagem da página.
 */
final class CatalogQueries {

    /** Campos que as listas aceitam para ordenar; o nome vem do cliente, então nunca entra solto na consulta. */
    private static final Set<String> SORTABLE = Set.of("name", "status");

    private CatalogQueries() {
    }

    /** {@link StatusFilter#ALL} não filtra; os outros filtram pela situação de mesmo nome. */
    static void filterByStatus(
            String alias,
            StatusFilter status,
            List<String> conditions,
            Map<String, Object> parameters
    ) {
        if (status == StatusFilter.ALL)
            return;

        conditions.add(alias + ".status = :status");
        parameters.put("status", CatalogStatus.valueOf(status.name()));
    }

    static boolean hasText(String text) {
        return text != null && !text.isBlank();
    }

    /** Padrão "contém", sem caixa, com {@code %}, {@code _} e a barra valendo como caracteres literais. */
    static String containing(String text) {
        var escaped = text.trim().toLowerCase(Locale.ROOT)
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");

        return "%" + escaped + "%";
    }

    static String where(List<String> conditions) {
        if (conditions.isEmpty())
            return "";

        return "where " + String.join(" and ", conditions) + " ";
    }

    static String orderBy(String alias, Pageable pageable) {
        var field = pageable.sortBy() != null && SORTABLE.contains(pageable.sortBy()) ? pageable.sortBy() : "name";
        var direction = "DESC".equalsIgnoreCase(pageable.sortDirection()) ? "desc" : "asc";

        return " order by " + alias + "." + field + " " + direction + ", " + alias + ".id";
    }

    static <T> Page<T> page(
            List<T> content,
            Pageable pageable,
            long total
    ) {
        var totalPages = pageable.size() <= 0 ? 0 : (int) Math.ceil((double) total / pageable.size());

        return new Page<>(
                content,
                pageable.page(),
                pageable.size(),
                total,
                totalPages
        );
    }
}
