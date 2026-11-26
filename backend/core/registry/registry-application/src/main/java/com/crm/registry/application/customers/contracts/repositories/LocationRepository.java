package com.crm.registry.application.customers.contracts.repositories;

import com.crm.common.application.repository.Repository;
import com.crm.registry.application.customers.criteria.LocationCriteria;
import com.crm.registry.domain.customers.entities.Location;

/** Persistência da entidade {@link Location}. */
public interface LocationRepository extends Repository<Location, LocationCriteria> {
}
