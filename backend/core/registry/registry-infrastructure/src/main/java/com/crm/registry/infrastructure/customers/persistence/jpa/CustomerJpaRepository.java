package com.crm.registry.infrastructure.customers.persistence.jpa;

import com.crm.registry.infrastructure.customers.persistence.entities.CustomerEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

/** Spring Data de {@link CustomerEntity}; só a infraestrutura o enxerga. */
public interface CustomerJpaRepository extends JpaRepository<CustomerEntity, UUID> {

    @Query("select count(c) > 0 from CustomerEntity c where c.document = :document")
    boolean existsByDocument(@Param("document") String document);
}
