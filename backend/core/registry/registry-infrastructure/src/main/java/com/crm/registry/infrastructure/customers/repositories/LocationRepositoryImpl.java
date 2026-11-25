package com.crm.registry.infrastructure.customers.repositories;

import com.crm.common.infrastructure.persistence.repositories.RepositoryImpl;
import com.crm.registry.application.customers.contracts.repositories.LocationRepository;
import com.crm.registry.application.customers.criteria.LocationCriteria;
import com.crm.registry.domain.customers.entities.Location;
import com.crm.registry.infrastructure.customers.mappers.LocationMapper;
import com.crm.registry.infrastructure.customers.persistence.entities.LocationEntity;
import com.crm.registry.infrastructure.customers.persistence.jpa.LocationJpaRepository;
import jakarta.persistence.EntityManager;

/** Repository de {@link Location} sobre JPA; o lock otimista é o da própria linha. */
public class LocationRepositoryImpl
        extends RepositoryImpl<Location, LocationCriteria, LocationEntity, LocationJpaRepository>
        implements LocationRepository {

    public LocationRepositoryImpl(
            LocationJpaRepository jpaRepository,
            EntityManager em,
            LocationMapper mapper
    ) {
        super(
                jpaRepository,
                em,
                mapper,
                LocationEntity.class
        );
    }
}
