package com.crm.registry.infrastructure.customers.persistence.jpa;

import com.crm.registry.infrastructure.customers.persistence.entities.InstalledEquipmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

/** Spring Data de {@link InstalledEquipmentEntity}; só a infraestrutura o enxerga. */
public interface InstalledEquipmentJpaRepository extends JpaRepository<InstalledEquipmentEntity, UUID> {

    @Query("""
            select count(e) > 0 from InstalledEquipmentEntity e
            where e.modelId = :modelId
              and lower(e.serialNumber) = lower(:serial)
              and (:exceptId is null or e.id <> :exceptId)
            """)
    boolean existsByModelAndSerialNumber(
            @Param("modelId") UUID modelId,
            @Param("serial") String serial,
            @Param("exceptId") UUID exceptId
    );
}
