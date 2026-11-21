package com.crm.registry.infrastructure.catalog.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

/** Spring Data de {@link EquipmentModelEntity}; só a infraestrutura o enxerga. */
public interface EquipmentModelJpaRepository extends JpaRepository<EquipmentModelEntity, UUID> {

    @Query("""
            select count(e) > 0 from EquipmentModelEntity e
            where e.manufacturerId = :manufacturerId
              and lower(e.name) = lower(:name)
              and (:exceptId is null or e.id <> :exceptId)
            """)
    boolean existsByManufacturerAndName(
            @Param("manufacturerId") UUID manufacturerId,
            @Param("name") String name,
            @Param("exceptId") UUID exceptId
    );
}
