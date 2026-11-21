package com.crm.registry.infrastructure.catalog.persistence;

import com.crm.common.application.pages.Page;
import com.crm.common.application.pages.Pageable;
import com.crm.registry.application.catalog.contracts.readers.EquipmentModelReader;
import com.crm.registry.application.catalog.schemas.EquipmentModelFilter;
import com.crm.registry.application.catalog.schemas.EquipmentModelSummary;
import com.crm.registry.application.catalog.schemas.EquipmentModelSummary.Ref;
import com.crm.registry.application.catalog.schemas.EquipmentModelSummary.ValueView;
import com.crm.registry.domain.catalog.vos.CatalogStatus;
import com.crm.registry.domain.catalog.vos.MeasurementUnit;
import jakarta.persistence.EntityManager;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Leitura de modelos direto em projeção, sem montar o aggregate: uma consulta pagina os modelos com
 * o nome do fabricante e do tipo, e outra traz os valores da página com o nome e a unidade da
 * característica. A busca de texto casa o nome do modelo e o do fabricante.
 */
public class JpaEquipmentModelReaderImpl implements EquipmentModelReader {

    private static final String FROM = """
            from EquipmentModelEntity e
            join ManufacturerEntity m on m.id = e.manufacturerId
            join EquipmentTypeEntity t on t.id = e.typeId
            """;

    private static final String SELECT = "select e.id, e.name, e.status, m.id, m.name, t.id, t.name " + FROM;

    private final EntityManager em;

    public JpaEquipmentModelReaderImpl(EntityManager em) {
        this.em = em;
    }

    @Override
    public Page<EquipmentModelSummary> search(
            EquipmentModelFilter filter,
            Pageable pageable
    ) {
        var conditions = new ArrayList<String>();
        var parameters = new HashMap<String, Object>();

        CatalogQueries.filterByStatus("e", filter.status(), conditions, parameters);

        if (filter.typeId() != null) {
            conditions.add("e.typeId = :typeId");
            parameters.put("typeId", filter.typeId());
        }

        if (CatalogQueries.hasText(filter.text())) {
            conditions.add("(lower(e.name) like :text escape '\\' or lower(m.name) like :text escape '\\')");
            parameters.put("text", CatalogQueries.containing(filter.text()));
        }

        var where = CatalogQueries.where(conditions);

        var query = em.createQuery(SELECT + where + CatalogQueries.orderBy("e", pageable), Object[].class);
        parameters.forEach(query::setParameter);
        query.setFirstResult(pageable.getOffset());
        query.setMaxResults(pageable.getPageSize());

        var count = em.createQuery("select count(e) " + FROM + where, Long.class);
        parameters.forEach(count::setParameter);

        return CatalogQueries.page(summaries(query.getResultList()), pageable, count.getSingleResult());
    }

    @Override
    public Optional<EquipmentModelSummary> findSummary(UUID id) {
        var rows = em.createQuery(SELECT + "where e.id = :id", Object[].class)
                .setParameter("id", id)
                .getResultList();

        return summaries(rows).stream().findFirst();
    }

    @Override
    public boolean hasModelsOfType(UUID typeId) {
        var found = em.createQuery("select e.id from EquipmentModelEntity e where e.typeId = :typeId", UUID.class)
                .setParameter("typeId", typeId)
                .setMaxResults(1)
                .getResultList();

        return !found.isEmpty();
    }

    private List<EquipmentModelSummary> summaries(List<Object[]> rows) {
        var values = valuesOf(rows.stream().map(row -> (UUID) row[0]).toList());

        return rows.stream()
                .map(row -> new EquipmentModelSummary(
                        (UUID) row[0],
                        (String) row[1],
                        (CatalogStatus) row[2],
                        new Ref((UUID) row[3], (String) row[4]),
                        new Ref((UUID) row[5], (String) row[6]),
                        values.getOrDefault((UUID) row[0], List.of())
                ))
                .toList();
    }

    private Map<UUID, List<ValueView>> valuesOf(List<UUID> modelIds) {
        if (modelIds.isEmpty())
            return Map.of();

        var rows = em.createQuery("""
                        select e.id, v.definitionId, d.name, d.unit, v.amount
                        from EquipmentModelEntity e
                        join e.values v
                        join CharacteristicDefinitionEntity d on d.id = v.definitionId
                        where e.id in :ids
                        order by d.name
                        """, Object[].class)
                .setParameter("ids", modelIds)
                .getResultList();

        return rows.stream().collect(Collectors.groupingBy(
                row -> (UUID) row[0],
                Collectors.mapping(row -> new ValueView(
                        (UUID) row[1],
                        (String) row[2],
                        (MeasurementUnit) row[3],
                        (BigDecimal) row[4]
                ), Collectors.toList())
        ));
    }
}
