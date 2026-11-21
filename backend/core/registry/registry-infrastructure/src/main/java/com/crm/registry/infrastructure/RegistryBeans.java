package com.crm.registry.infrastructure;

import com.crm.registry.application.catalog.contracts.readers.EquipmentModelReader;
import com.crm.registry.application.catalog.contracts.readers.EquipmentTypeReader;
import com.crm.registry.application.catalog.contracts.readers.ManufacturerReader;
import com.crm.registry.application.catalog.contracts.repositories.EquipmentModelRepository;
import com.crm.registry.application.catalog.contracts.repositories.EquipmentTypeRepository;
import com.crm.registry.application.catalog.contracts.repositories.ManufacturerRepository;
import com.crm.registry.application.catalog.usecases.commands.DeactivateEquipmentModel;
import com.crm.registry.application.catalog.usecases.commands.DeactivateEquipmentType;
import com.crm.registry.application.catalog.usecases.commands.DeactivateManufacturer;
import com.crm.registry.application.catalog.usecases.commands.ReactivateEquipmentModel;
import com.crm.registry.application.catalog.usecases.commands.ReactivateEquipmentType;
import com.crm.registry.application.catalog.usecases.commands.ReactivateManufacturer;
import com.crm.registry.application.catalog.usecases.commands.RegisterEquipmentModel;
import com.crm.registry.application.catalog.usecases.commands.RegisterEquipmentType;
import com.crm.registry.application.catalog.usecases.commands.RegisterManufacturer;
import com.crm.registry.application.catalog.usecases.commands.RenameManufacturer;
import com.crm.registry.application.catalog.usecases.commands.UpdateEquipmentModel;
import com.crm.registry.application.catalog.usecases.commands.UpdateEquipmentType;
import com.crm.registry.application.catalog.usecases.queries.GetEquipmentModel;
import com.crm.registry.application.catalog.usecases.queries.ListEquipmentModels;
import com.crm.registry.application.catalog.usecases.queries.ListEquipmentTypes;
import com.crm.registry.application.catalog.usecases.queries.ListManufacturers;
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
import com.crm.registry.infrastructure.catalog.seed.CatalogSeeder;
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

    // --- catálogo: commands -------------------------------------------------------------------

    @Bean
    public RegisterManufacturer registerManufacturer(ManufacturerRepository manufacturerRepository) {
        return new RegisterManufacturer(manufacturerRepository);
    }

    @Bean
    public RenameManufacturer renameManufacturer(ManufacturerRepository manufacturerRepository) {
        return new RenameManufacturer(manufacturerRepository);
    }

    @Bean
    public DeactivateManufacturer deactivateManufacturer(ManufacturerRepository manufacturerRepository) {
        return new DeactivateManufacturer(manufacturerRepository);
    }

    @Bean
    public ReactivateManufacturer reactivateManufacturer(ManufacturerRepository manufacturerRepository) {
        return new ReactivateManufacturer(manufacturerRepository);
    }

    @Bean
    public RegisterEquipmentType registerEquipmentType(EquipmentTypeRepository equipmentTypeRepository) {
        return new RegisterEquipmentType(equipmentTypeRepository);
    }

    @Bean
    public UpdateEquipmentType updateEquipmentType(
            EquipmentTypeRepository equipmentTypeRepository,
            EquipmentModelReader equipmentModelReader
    ) {
        return new UpdateEquipmentType(equipmentTypeRepository, equipmentModelReader);
    }

    @Bean
    public DeactivateEquipmentType deactivateEquipmentType(EquipmentTypeRepository equipmentTypeRepository) {
        return new DeactivateEquipmentType(equipmentTypeRepository);
    }

    @Bean
    public ReactivateEquipmentType reactivateEquipmentType(EquipmentTypeRepository equipmentTypeRepository) {
        return new ReactivateEquipmentType(equipmentTypeRepository);
    }

    @Bean
    public RegisterEquipmentModel registerEquipmentModel(
            EquipmentModelRepository equipmentModelRepository,
            ManufacturerRepository manufacturerRepository,
            EquipmentTypeRepository equipmentTypeRepository
    ) {
        return new RegisterEquipmentModel(
                equipmentModelRepository,
                manufacturerRepository,
                equipmentTypeRepository
        );
    }

    @Bean
    public UpdateEquipmentModel updateEquipmentModel(
            EquipmentModelRepository equipmentModelRepository,
            EquipmentTypeRepository equipmentTypeRepository
    ) {
        return new UpdateEquipmentModel(equipmentModelRepository, equipmentTypeRepository);
    }

    @Bean
    public DeactivateEquipmentModel deactivateEquipmentModel(EquipmentModelRepository equipmentModelRepository) {
        return new DeactivateEquipmentModel(equipmentModelRepository);
    }

    @Bean
    public ReactivateEquipmentModel reactivateEquipmentModel(EquipmentModelRepository equipmentModelRepository) {
        return new ReactivateEquipmentModel(equipmentModelRepository);
    }

    // --- catálogo: queries --------------------------------------------------------------------

    @Bean
    public ListManufacturers listManufacturers(ManufacturerReader manufacturerReader) {
        return new ListManufacturers(manufacturerReader);
    }

    @Bean
    public ListEquipmentTypes listEquipmentTypes(EquipmentTypeReader equipmentTypeReader) {
        return new ListEquipmentTypes(equipmentTypeReader);
    }

    @Bean
    public ListEquipmentModels listEquipmentModels(EquipmentModelReader equipmentModelReader) {
        return new ListEquipmentModels(equipmentModelReader);
    }

    @Bean
    public GetEquipmentModel getEquipmentModel(EquipmentModelReader equipmentModelReader) {
        return new GetEquipmentModel(equipmentModelReader);
    }

    // --- seed ---------------------------------------------------------------------------------

    @Bean
    public CatalogSeeder catalogSeeder(
            ManufacturerRepository manufacturerRepository,
            EquipmentTypeRepository equipmentTypeRepository,
            EquipmentModelRepository equipmentModelRepository
    ) {
        return new CatalogSeeder(
                manufacturerRepository,
                equipmentTypeRepository,
                equipmentModelRepository
        );
    }
}
