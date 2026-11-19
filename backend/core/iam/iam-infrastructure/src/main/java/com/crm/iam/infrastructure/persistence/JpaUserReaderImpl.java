package com.crm.iam.infrastructure.persistence;

import com.crm.common.application.pages.Page;
import com.crm.common.application.pages.Pageable;
import com.crm.iam.application.contracts.readers.UserReader;
import com.crm.iam.application.schemas.UserFilter;
import com.crm.iam.application.schemas.UserSummary;
import jakarta.persistence.EntityManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Leitura de usuários direto em projeção: uma consulta só, com o último uso calculado como o
 * maior {@code last_used_at} das sessões do usuário, sem montar o aggregate.
 */
public class JpaUserReaderImpl implements UserReader {

    private static final String SELECT = """
            select new com.crm.iam.application.schemas.UserSummary(
                u.id, u.name, u.email, u.role, u.status,
                (select max(s.lastUsedAt) from SessionEntity s where s.userId = u.id)
            )
            from UserEntity u
            """;

    /** Campos que a lista aceita para ordenar; o nome vem do cliente, então nunca entra solto na consulta. */
    private static final Set<String> SORTABLE = Set.of("name", "email", "role", "status");

    private final EntityManager em;

    public JpaUserReaderImpl(EntityManager em) {
        this.em = em;
    }

    @Override
    public Page<UserSummary> search(
            UserFilter filter,
            Pageable pageable
    ) {
        var parameters = new HashMap<String, Object>();
        var where = where(filter, parameters);

        var query = em.createQuery(SELECT + where + orderBy(pageable), UserSummary.class);
        parameters.forEach(query::setParameter);
        query.setFirstResult(pageable.getOffset());
        query.setMaxResults(pageable.getPageSize());

        var count = em.createQuery("select count(u) from UserEntity u " + where, Long.class);
        parameters.forEach(count::setParameter);

        var total = count.getSingleResult();

        return new Page<>(
                query.getResultList(),
                pageable.page(),
                pageable.size(),
                total,
                totalPages(total, pageable.size())
        );
    }

    @Override
    public Optional<UserSummary> findSummary(UUID id) {
        return em.createQuery(SELECT + "where u.id = :id", UserSummary.class)
                .setParameter("id", id)
                .getResultList().stream()
                .findFirst();
    }

    private String where(
            UserFilter filter,
            Map<String, Object> parameters
    ) {
        var conditions = new ArrayList<String>();

        if (filter.role() != null) {
            conditions.add("u.role = :role");
            parameters.put("role", filter.role());
        }

        if (filter.status() != null) {
            conditions.add("u.status = :status");
            parameters.put("status", filter.status());
        }

        if (filter.text() != null && !filter.text().isBlank()) {
            conditions.add("(lower(u.name) like :text escape '\\' or lower(u.email) like :text escape '\\')");
            parameters.put("text", "%" + escapeLike(filter.text().trim().toLowerCase()) + "%");
        }

        if (conditions.isEmpty())
            return "";

        return "where " + String.join(" and ", conditions) + " ";
    }

    /** Faz {@code %}, {@code _} e a própria barra valerem como caracteres literais no LIKE. */
    private String escapeLike(String text) {
        return text
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }

    private String orderBy(Pageable pageable) {
        var field = pageable.sortBy() != null && SORTABLE.contains(pageable.sortBy()) ? pageable.sortBy() : "name";
        var direction = "DESC".equalsIgnoreCase(pageable.sortDirection()) ? "desc" : "asc";

        return " order by u." + field + " " + direction + ", u.id";
    }

    private int totalPages(long total, int size) {
        if (size <= 0)
            return 0;

        return (int) Math.ceil((double) total / size);
    }
}
