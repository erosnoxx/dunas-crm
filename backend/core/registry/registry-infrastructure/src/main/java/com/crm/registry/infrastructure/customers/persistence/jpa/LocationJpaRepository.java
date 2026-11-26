package com.crm.registry.infrastructure.customers.persistence.jpa;

import com.crm.registry.infrastructure.customers.persistence.entities.LocationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/** Spring Data de {@link LocationEntity}; só a infraestrutura o enxerga. */
public interface LocationJpaRepository extends JpaRepository<LocationEntity, UUID> {
}
