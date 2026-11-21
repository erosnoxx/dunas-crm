package com.crm.registry.infrastructure.catalog.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

/** Spring Data de {@link ManufacturerEntity}; só a infraestrutura o enxerga. */
public interface ManufacturerJpaRepository extends JpaRepository<ManufacturerEntity, UUID> {

    @Query("""
            select count(m) > 0 from ManufacturerEntity m
            where lower(m.name) = lower(:name)
              and (:exceptId is null or m.id <> :exceptId)
            """)
    boolean existsByName(@Param("name") String name, @Param("exceptId") UUID exceptId);
}
