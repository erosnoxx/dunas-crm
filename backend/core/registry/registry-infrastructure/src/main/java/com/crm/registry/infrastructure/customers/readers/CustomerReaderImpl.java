package com.crm.registry.infrastructure.customers.readers;

import com.crm.common.application.pages.Page;
import com.crm.common.application.pages.Pageable;
import com.crm.registry.application.customers.contracts.readers.CustomerReader;
import com.crm.registry.application.customers.schemas.CustomerDetail;
import com.crm.registry.application.customers.schemas.CustomerDetail.AddressView;
import com.crm.registry.application.customers.schemas.CustomerDetail.CharacteristicView;
import com.crm.registry.application.customers.schemas.CustomerDetail.EquipmentView;
import com.crm.registry.application.customers.schemas.CustomerDetail.LocationView;
import com.crm.registry.application.customers.schemas.CustomerDetail.ModelRef;
import com.crm.registry.application.customers.schemas.CustomerDetail.Ref;
import com.crm.registry.application.customers.schemas.CustomerFilter;
import com.crm.registry.application.customers.schemas.CustomerStatusFilter;
import com.crm.registry.application.customers.schemas.CustomerSummary;
import com.crm.registry.application.customers.schemas.CustomerSummary.CityView;
import com.crm.registry.application.customers.schemas.CustomerSummary.MatchedSerial;
import com.crm.registry.domain.catalog.enums.CatalogStatus;
import com.crm.registry.domain.catalog.enums.MeasurementUnit;
import com.crm.registry.domain.customers.enums.CustomerStatus;
import com.crm.registry.infrastructure.catalog.readers.CatalogQueries;
import com.crm.registry.infrastructure.customers.persistence.entities.CustomerEntity;
import com.crm.registry.infrastructure.customers.persistence.entities.LocationEntity;
import com.crm.shared.domain.enums.UF;
import com.crm.shared.domain.vos.identity.Document;
import jakarta.persistence.EntityManager;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Leitura de clientes direto em projeção, sem montar o aggregate. A lista é uma consulta paginada
 * sem junção (as contagens saem por subconsulta, então um cliente é sempre uma linha), mais uma
 * consulta das cidades da página e, só com texto, uma das séries que casaram (ADR-27). A busca de
 * texto é a do catálogo (ADR-15).
 */
public class CustomerReaderImpl implements CustomerReader {

    private static final String ACTIVE = "com.crm.registry.domain.customers.enums.CustomerStatus.ACTIVE";

    /** Séries casadas mostradas por cliente na lista (ADR-27). */
    private static final int MATCHED_SERIALS = 3;

    private static final String FROM = "from CustomerEntity c ";

    private static final String SELECT = """
            select c.id, c.name, c.tradeName, c.document, c.status,
                   (select count(l) from LocationEntity l where l.customerId = c.id and l.status = %1$s),
                   (select count(e) from InstalledEquipmentEntity e join LocationEntity l on l.id = e.locationId
                    where l.customerId = c.id and l.status = %1$s and e.status = %1$s)
            """.formatted(ACTIVE) + FROM;

    private static final String TEXT = """
            (lower(c.name) like :text escape '\\'
             or lower(c.tradeName) like :text escape '\\'
             or exists (select 1 from LocationEntity l
                        where l.customerId = c.id and l.status = %1$s and lower(l.city) like :text escape '\\')
             or exists (select 1 from InstalledEquipmentEntity e join LocationEntity l on l.id = e.locationId
                        where l.customerId = c.id and e.status = %1$s and lower(e.serialNumber) like :text escape '\\')
            """.formatted(ACTIVE);

    private final EntityManager em;

    public CustomerReaderImpl(EntityManager em) {
        this.em = em;
    }

    @Override
    public Page<CustomerSummary> search(
            CustomerFilter filter,
            Pageable pageable
    ) {
        var conditions = new ArrayList<String>();
        var parameters = new HashMap<String, Object>();

        if (filter.status() != CustomerStatusFilter.ALL) {
            conditions.add("c.status = :status");
            parameters.put("status", CustomerStatus.valueOf(filter.status().name()));
        }

        if (CatalogQueries.hasText(filter.text())) {
            conditions.add(textCondition(filter.text(), parameters));
            parameters.put("text", CatalogQueries.containing(filter.text()));
        }

        var where = CatalogQueries.where(conditions);

        var query = em.createQuery(SELECT + where + CatalogQueries.orderBy("c", pageable), Object[].class);
        parameters.forEach(query::setParameter);
        query.setFirstResult(pageable.getOffset());
        query.setMaxResults(pageable.getPageSize());
        var rows = query.getResultList();

        var count = em.createQuery("select count(c) " + FROM + where, Long.class);
        parameters.forEach(count::setParameter);

        var ids = rows.stream().map(row -> (UUID) row[0]).toList();
        var cities = citiesOf(ids);
        var serials = matchedSerialsOf(ids, filter.text());

        var content = rows.stream()
                .map(row -> new CustomerSummary(
                        (UUID) row[0],
                        (String) row[1],
                        (String) row[2],
                        (String) row[3],
                        Document.of((String) row[3]).getType(),
                        (CustomerStatus) row[4],
                        (Long) row[5],
                        (Long) row[6],
                        cities.getOrDefault((UUID) row[0], List.of()),
                        serials.getOrDefault((UUID) row[0], List.of())
                ))
                .toList();

        return CatalogQueries.page(content, pageable, count.getSingleResult());
    }

    /** O documento só entra quando sobra letra ou dígito: "%" sozinho não pode casar todo documento. */
    private static String textCondition(String text, Map<String, Object> parameters) {
        var document = text.replaceAll("[^\\p{Alnum}]", "").toUpperCase(Locale.ROOT);

        if (document.isEmpty())
            return TEXT + ")";

        parameters.put("document", "%" + document + "%");

        return TEXT + " or c.document like :document)";
    }

    private Map<UUID, List<CityView>> citiesOf(List<UUID> customerIds) {
        if (customerIds.isEmpty())
            return Map.of();

        var rows = em.createQuery("""
                        select distinct l.customerId, l.city, l.state
                        from LocationEntity l
                        where l.customerId in :ids and l.status = %s
                        order by l.city
                        """.formatted(ACTIVE), Object[].class)
                .setParameter("ids", customerIds)
                .getResultList();

        return rows.stream().collect(Collectors.groupingBy(
                row -> (UUID) row[0],
                Collectors.mapping(row -> new CityView((String) row[1], (UF) row[2]), Collectors.toList())
        ));
    }

    /** Só com texto: sem ele, ou sem série que case, o cliente fica sem séries casadas (ADR-27). */
    private Map<UUID, List<MatchedSerial>> matchedSerialsOf(List<UUID> customerIds, String text) {
        if (customerIds.isEmpty() || !CatalogQueries.hasText(text))
            return Map.of();

        var rows = em.createQuery("""
                        select l.customerId, e.serialNumber, m.name
                        from InstalledEquipmentEntity e
                        join LocationEntity l on l.id = e.locationId
                        join EquipmentModelEntity m on m.id = e.modelId
                        where l.customerId in :ids and e.status = %s and lower(e.serialNumber) like :text escape '\\'
                        order by e.serialNumber, m.name
                        """.formatted(ACTIVE), Object[].class)
                .setParameter("ids", customerIds)
                .setParameter("text", CatalogQueries.containing(text))
                .getResultList();

        return rows.stream().collect(Collectors.groupingBy(
                row -> (UUID) row[0],
                Collectors.collectingAndThen(
                        Collectors.mapping(row -> new MatchedSerial((String) row[1], (String) row[2]), Collectors.toList()),
                        serials -> serials.stream().limit(MATCHED_SERIALS).toList()
                )
        ));
    }

    @Override
    public Optional<CustomerDetail> findDetail(UUID customerId) {
        var customer = em.find(CustomerEntity.class, customerId);

        if (customer == null)
            return Optional.empty();

        var locations = em.createQuery("""
                        select l from LocationEntity l
                        where l.customerId = :id
                        order by case when l.status = %s then 0 else 1 end, lower(l.name)
                        """.formatted(ACTIVE), LocationEntity.class)
                .setParameter("id", customerId)
                .getResultList();
        var equipment = equipmentOf(customerId);

        var views = locations.stream()
                .map(location -> new LocationView(
                        location.getId(),
                        location.getName(),
                        location.getStatus(),
                        new AddressView(
                                location.getZipCode(),
                                location.getStreet(),
                                location.getNumber(),
                                location.getComplement(),
                                location.getNeighborhood(),
                                location.getCity(),
                                location.getState()
                        ),
                        equipment.getOrDefault(location.getId(), List.of())
                ))
                .toList();

        return Optional.of(new CustomerDetail(
                customer.getId(),
                customer.getDocument(),
                Document.of(customer.getDocument()).getType(),
                customer.getName(),
                customer.getTradeName(),
                customer.getEmail(),
                customer.getPhone(),
                customer.getStatus(),
                views
        ));
    }

    /** Equipamentos de todos os locais do cliente, por local; a característica vem nula se o modelo não tem valor. */
    private Map<UUID, List<EquipmentView>> equipmentOf(UUID customerId) {
        var rows = em.createQuery("""
                        select e.locationId, e.id, e.serialNumber, e.installedOn, e.status,
                               m.id, m.name, m.status, f.id, f.name, t.id, t.name,
                               d.name, d.unit, v.amount
                        from InstalledEquipmentEntity e
                        join LocationEntity l on l.id = e.locationId
                        join EquipmentModelEntity m on m.id = e.modelId
                        join ManufacturerEntity f on f.id = m.manufacturerId
                        join EquipmentTypeEntity t on t.id = m.typeId
                        left join m.values v
                        left join CharacteristicDefinitionEntity d on d.id = v.definitionId
                        where l.customerId = :id
                        order by case when e.status = %s then 0 else 1 end, lower(t.name), lower(m.name), e.serialNumber
                        """.formatted(ACTIVE), Object[].class)
                .setParameter("id", customerId)
                .getResultList();

        return rows.stream().collect(Collectors.groupingBy(
                row -> (UUID) row[0],
                Collectors.mapping(CustomerReaderImpl::equipmentView, Collectors.toList())
        ));
    }

    private static EquipmentView equipmentView(Object[] row) {
        return new EquipmentView(
                (UUID) row[1],
                (String) row[2],
                (LocalDate) row[3],
                (CustomerStatus) row[4],
                new ModelRef(
                        (UUID) row[5],
                        (String) row[6],
                        (CatalogStatus) row[7]
                ),
                new Ref((UUID) row[8], (String) row[9]),
                new Ref((UUID) row[10], (String) row[11]),
                characteristicOf(row)
        );
    }

    private static CharacteristicView characteristicOf(Object[] row) {
        if (row[12] == null)
            return null;

        return new CharacteristicView(
                (String) row[12],
                (MeasurementUnit) row[13],
                (BigDecimal) row[14]
        );
    }
}
