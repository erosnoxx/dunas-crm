package com.crm.registry.infrastructure.catalog.persistence;

import com.crm.common.application.pages.Page;
import com.crm.common.application.pages.Pageable;
import com.crm.registry.application.catalog.contracts.readers.ManufacturerReader;
import com.crm.registry.application.catalog.schemas.ManufacturerFilter;
import com.crm.registry.application.catalog.schemas.ManufacturerSummary;
import jakarta.persistence.EntityManager;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Leitura de fabricantes direto em projeção: uma consulta só, com a contagem de modelos (ativos ou
 * não) numa subconsulta, sem montar o aggregate.
 */
public class JpaManufacturerReaderImpl implements ManufacturerReader {

    private static final String SELECT = """
            select new com.crm.registry.application.catalog.schemas.ManufacturerSummary(
                m.id, m.name, m.status,
                (select count(e) from EquipmentModelEntity e where e.manufacturerId = m.id)
            )
            from ManufacturerEntity m
            """;

    private final EntityManager em;

    public JpaManufacturerReaderImpl(EntityManager em) {
        this.em = em;
    }

    @Override
    public Page<ManufacturerSummary> search(
            ManufacturerFilter filter,
            Pageable pageable
    ) {
        var conditions = new ArrayList<String>();
        var parameters = new HashMap<String, Object>();

        CatalogQueries.filterByStatus("m", filter.status(), conditions, parameters);

        if (CatalogQueries.hasText(filter.text())) {
            conditions.add("lower(m.name) like :text escape '\\'");
            parameters.put("text", CatalogQueries.containing(filter.text()));
        }

        var where = CatalogQueries.where(conditions);

        var query = em.createQuery(SELECT + where + CatalogQueries.orderBy("m", pageable), ManufacturerSummary.class);
        parameters.forEach(query::setParameter);
        query.setFirstResult(pageable.getOffset());
        query.setMaxResults(pageable.getPageSize());

        var count = em.createQuery("select count(m) from ManufacturerEntity m " + where, Long.class);
        parameters.forEach(count::setParameter);

        return CatalogQueries.page(query.getResultList(), pageable, count.getSingleResult());
    }
}
