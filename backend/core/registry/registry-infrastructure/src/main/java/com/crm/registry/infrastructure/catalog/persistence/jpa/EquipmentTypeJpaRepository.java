package com.crm.registry.infrastructure.catalog.persistence.jpa;

import com.crm.registry.infrastructure.catalog.persistence.entities.EquipmentTypeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

/** Spring Data de {@link EquipmentTypeEntity}; só a infraestrutura o enxerga. */
public interface EquipmentTypeJpaRepository extends JpaRepository<EquipmentTypeEntity, UUID> {

    @Query("""
            select count(t) > 0 from EquipmentTypeEntity t
            where lower(t.name) = lower(:name)
              and (:exceptId is null or t.id <> :exceptId)
            """)
    boolean existsByName(@Param("name") String name, @Param("exceptId") UUID exceptId);
}
