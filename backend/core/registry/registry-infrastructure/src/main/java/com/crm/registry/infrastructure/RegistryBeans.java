package com.crm.registry.infrastructure;

import com.crm.registry.application.catalog.contracts.readers.EquipmentModelReader;
import com.crm.registry.application.catalog.contracts.readers.EquipmentTypeReader;
import com.crm.registry.application.catalog.contracts.readers.ManufacturerReader;
import com.crm.registry.application.catalog.contracts.repositories.EquipmentModelRepository;
import com.crm.registry.application.catalog.contracts.repositories.EquipmentTypeRepository;
import com.crm.registry.application.catalog.contracts.repositories.ManufacturerRepository;
import com.crm.registry.infrastructure.catalog.persistence.EquipmentModelJpaRepository;
import com.crm.registry.infrastructure.catalog.persistence.EquipmentModelMapper;
import com.crm.registry.infrastructure.catalog.persistence.EquipmentTypeJpaRepository;
import com.crm.registry.infrastructure.catalog.persistence.EquipmentTypeMapper;
import com.crm.registry.infrastructure.catalog.persistence.JpaEquipmentModelReaderImpl;
import com.crm.registry.infrastructure.catalog.persistence.JpaEquipmentModelRepositoryImpl;
import com.crm.registry.infrastructure.catalog.persistence.JpaEquipmentTypeReaderImpl;
import com.crm.registry.infrastructure.catalog.persistence.JpaEquipmentTypeRepositoryImpl;
import com.crm.registry.infrastructure.catalog.persistence.JpaManufacturerReaderImpl;
import com.crm.registry.infrastructure.catalog.persistence.JpaManufacturerRepositoryImpl;
import com.crm.registry.infrastructure.catalog.persistence.ManufacturerJpaRepository;
import com.crm.registry.infrastructure.catalog.persistence.ManufacturerMapper;
import jakarta.persistence.EntityManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registra todos os beans do BC {@code registry} (architecture.md §9): sem {@code @Component} nem
 * {@code @Service}, use case, repository e listener são instanciados aqui, com {@code new}.
 *
 * <p>Os contratos de {@code registry-application} são devolvidos pelo tipo que os use cases pedem.
 */
@Configuration
public class RegistryBeans {

    // --- catálogo: persistência ---------------------------------------------------------------

    @Bean
    public ManufacturerMapper manufacturerMapper() {
        return new ManufacturerMapper();
    }

    @Bean
    public EquipmentTypeMapper equipmentTypeMapper() {
        return new EquipmentTypeMapper();
    }

    @Bean
    public EquipmentModelMapper equipmentModelMapper() {
        return new EquipmentModelMapper();
    }

    @Bean
    public ManufacturerRepository manufacturerRepository(
            ManufacturerJpaRepository jpaRepository,
            EntityManager em,
            ManufacturerMapper mapper
    ) {
        return new JpaManufacturerRepositoryImpl(
                jpaRepository,
                em,
                mapper
        );
    }

    @Bean
    public EquipmentTypeRepository equipmentTypeRepository(
            EquipmentTypeJpaRepository jpaRepository,
            EntityManager em,
            EquipmentTypeMapper mapper
    ) {
        return new JpaEquipmentTypeRepositoryImpl(
                jpaRepository,
                em,
                mapper
        );
    }

    @Bean
    public EquipmentModelRepository equipmentModelRepository(
            EquipmentModelJpaRepository jpaRepository,
            EntityManager em,
            EquipmentModelMapper mapper
    ) {
        return new JpaEquipmentModelRepositoryImpl(
                jpaRepository,
                em,
                mapper
        );
    }

    @Bean
    public ManufacturerReader manufacturerReader(EntityManager em) {
        return new JpaManufacturerReaderImpl(em);
    }

    @Bean
    public EquipmentTypeReader equipmentTypeReader(EntityManager em) {
        return new JpaEquipmentTypeReaderImpl(em);
    }

    @Bean
    public EquipmentModelReader equipmentModelReader(EntityManager em) {
        return new JpaEquipmentModelReaderImpl(em);
    }
}
