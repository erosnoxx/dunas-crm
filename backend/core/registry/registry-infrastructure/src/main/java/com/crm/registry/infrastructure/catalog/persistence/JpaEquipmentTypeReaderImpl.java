package com.crm.registry.infrastructure.catalog.persistence;

import com.crm.common.application.pages.Page;
import com.crm.common.application.pages.Pageable;
import com.crm.registry.application.catalog.contracts.readers.EquipmentTypeReader;
import com.crm.registry.application.catalog.schemas.EquipmentTypeSummary;
import com.crm.registry.application.catalog.schemas.EquipmentTypeSummary.CharacteristicView;
import com.crm.registry.application.catalog.schemas.StatusFilter;
import com.crm.registry.domain.catalog.vos.CatalogStatus;
import com.crm.registry.domain.catalog.vos.MeasurementUnit;
import jakarta.persistence.EntityManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Leitura de tipos direto em projeção, sem montar o aggregate: uma consulta pagina os tipos, com a
 * contagem de modelos (ativos ou não) numa subconsulta, e outra traz as características da página.
 */
public class JpaEquipmentTypeReaderImpl implements EquipmentTypeReader {

    private static final String SELECT = """
            select t.id, t.name, t.status,
                (select count(e) from EquipmentModelEntity e where e.typeId = t.id)
            from EquipmentTypeEntity t
            """;

    private final EntityManager em;

    public JpaEquipmentTypeReaderImpl(EntityManager em) {
        this.em = em;
    }

    @Override
    public Page<EquipmentTypeSummary> search(
            StatusFilter status,
            Pageable pageable
    ) {
        var conditions = new ArrayList<String>();
        var parameters = new HashMap<String, Object>();

        CatalogQueries.filterByStatus("t", status, conditions, parameters);

        var where = CatalogQueries.where(conditions);

        var query = em.createQuery(SELECT + where + CatalogQueries.orderBy("t", pageable), Object[].class);
        parameters.forEach(query::setParameter);
        query.setFirstResult(pageable.getOffset());
        query.setMaxResults(pageable.getPageSize());

        var rows = query.getResultList();
        var characteristics = characteristicsOf(rows.stream().map(row -> (UUID) row[0]).toList());

        var content = rows.stream()
                .map(row -> new EquipmentTypeSummary(
                        (UUID) row[0],
                        (String) row[1],
                        (CatalogStatus) row[2],
                        (Long) row[3],
                        characteristics.getOrDefault((UUID) row[0], List.of())
                ))
                .toList();

        var count = em.createQuery("select count(t) from EquipmentTypeEntity t " + where, Long.class);
        parameters.forEach(count::setParameter);

        return CatalogQueries.page(content, pageable, count.getSingleResult());
    }

    private Map<UUID, List<CharacteristicView>> characteristicsOf(List<UUID> typeIds) {
        if (typeIds.isEmpty())
            return Map.of();

        var rows = em.createQuery("""
                        select t.id, d.id, d.name, d.unit, d.required
                        from EquipmentTypeEntity t join t.characteristics d
                        where t.id in :ids
                        order by d.name
                        """, Object[].class)
                .setParameter("ids", typeIds)
                .getResultList();

        return rows.stream().collect(Collectors.groupingBy(
                row -> (UUID) row[0],
                Collectors.mapping(row -> new CharacteristicView(
                        (UUID) row[1],
                        (String) row[2],
                        (MeasurementUnit) row[3],
                        (Boolean) row[4]
                ), Collectors.toList())
        ));
    }
}
