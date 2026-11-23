package com.crm.registry.infrastructure.config;

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
import com.crm.registry.infrastructure.catalog.persistence.jpa.EquipmentModelJpaRepository;
import com.crm.registry.infrastructure.catalog.mappers.EquipmentModelMapper;
import com.crm.registry.infrastructure.catalog.persistence.jpa.EquipmentTypeJpaRepository;
import com.crm.registry.infrastructure.catalog.mappers.EquipmentTypeMapper;
import com.crm.registry.infrastructure.catalog.readers.EquipmentModelReaderImpl;
import com.crm.registry.infrastructure.catalog.repositories.EquipmentModelRepositoryImpl;
import com.crm.registry.infrastructure.catalog.readers.EquipmentTypeReaderImpl;
import com.crm.registry.infrastructure.catalog.repositories.EquipmentTypeRepositoryImpl;
import com.crm.registry.infrastructure.catalog.readers.ManufacturerReaderImpl;
import com.crm.registry.infrastructure.catalog.repositories.ManufacturerRepositoryImpl;
import com.crm.registry.infrastructure.catalog.persistence.jpa.ManufacturerJpaRepository;
import com.crm.registry.infrastructure.catalog.mappers.ManufacturerMapper;
import com.crm.registry.infrastructure.catalog.seed.CatalogSeeder;
import com.crm.registry.application.customers.contracts.readers.CustomerReader;
import com.crm.registry.application.customers.contracts.repositories.CustomerRepository;
import com.crm.registry.application.customers.contracts.repositories.InstalledEquipmentRepository;
import com.crm.registry.application.customers.contracts.repositories.LocationRepository;
import com.crm.registry.infrastructure.customers.mappers.CustomerMapper;
import com.crm.registry.infrastructure.customers.mappers.InstalledEquipmentMapper;
import com.crm.registry.infrastructure.customers.mappers.LocationMapper;
import com.crm.registry.infrastructure.customers.persistence.jpa.CustomerJpaRepository;
import com.crm.registry.infrastructure.customers.persistence.jpa.InstalledEquipmentJpaRepository;
import com.crm.registry.infrastructure.customers.persistence.jpa.LocationJpaRepository;
import com.crm.registry.infrastructure.customers.readers.CustomerReaderImpl;
import com.crm.registry.infrastructure.customers.repositories.CustomerRepositoryImpl;
import com.crm.registry.infrastructure.customers.repositories.InstalledEquipmentRepositoryImpl;
import com.crm.registry.infrastructure.customers.repositories.LocationRepositoryImpl;
import com.crm.registry.application.customers.usecases.commands.DeactivateCustomer;
import com.crm.registry.application.customers.usecases.commands.DeactivateInstalledEquipment;
import com.crm.registry.application.customers.usecases.commands.DeactivateLocation;
import com.crm.registry.application.customers.usecases.commands.InstallEquipment;
import com.crm.registry.application.customers.usecases.commands.ReactivateCustomer;
import com.crm.registry.application.customers.usecases.commands.ReactivateInstalledEquipment;
import com.crm.registry.application.customers.usecases.commands.ReactivateLocation;
import com.crm.registry.application.customers.usecases.commands.RegisterCustomer;
import com.crm.registry.application.customers.usecases.commands.RegisterLocation;
import com.crm.registry.application.customers.usecases.commands.UpdateCustomer;
import com.crm.registry.application.customers.usecases.commands.UpdateInstalledEquipment;
import com.crm.registry.application.customers.usecases.commands.UpdateLocation;
import com.crm.registry.application.customers.usecases.queries.GetCustomer;
import com.crm.registry.application.customers.usecases.queries.ListCustomers;
import jakarta.persistence.EntityManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

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
        return new ManufacturerRepositoryImpl(
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
        return new EquipmentTypeRepositoryImpl(
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
        return new EquipmentModelRepositoryImpl(
                jpaRepository,
                em,
                mapper
        );
    }

    @Bean
    public ManufacturerReader manufacturerReader(EntityManager em) {
        return new ManufacturerReaderImpl(em);
    }

    @Bean
    public EquipmentTypeReader equipmentTypeReader(EntityManager em) {
        return new EquipmentTypeReaderImpl(em);
    }

    @Bean
    public EquipmentModelReader equipmentModelReader(EntityManager em) {
        return new EquipmentModelReaderImpl(em);
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

    // --- clientes: persistência ---------------------------------------------------------------

    @Bean
    public CustomerMapper customerMapper() {
        return new CustomerMapper();
    }

    @Bean
    public LocationMapper locationMapper() {
        return new LocationMapper();
    }

    @Bean
    public InstalledEquipmentMapper installedEquipmentMapper() {
        return new InstalledEquipmentMapper();
    }

    @Bean
    public CustomerRepository customerRepository(
            CustomerJpaRepository jpaRepository,
            EntityManager em,
            CustomerMapper mapper
    ) {
        return new CustomerRepositoryImpl(
                jpaRepository,
                em,
                mapper
        );
    }

    @Bean
    public LocationRepository locationRepository(
            LocationJpaRepository jpaRepository,
            EntityManager em,
            LocationMapper mapper
    ) {
        return new LocationRepositoryImpl(
                jpaRepository,
                em,
                mapper
        );
    }

    @Bean
    public InstalledEquipmentRepository installedEquipmentRepository(
            InstalledEquipmentJpaRepository jpaRepository,
            EntityManager em,
            InstalledEquipmentMapper mapper
    ) {
        return new InstalledEquipmentRepositoryImpl(
                jpaRepository,
                em,
                mapper
        );
    }

    @Bean
    public CustomerReader customerReader(EntityManager em) {
        return new CustomerReaderImpl(em);
    }

    // --- clientes: commands -------------------------------------------------------------------

    @Bean
    public RegisterCustomer registerCustomer(CustomerRepository customerRepository) {
        return new RegisterCustomer(customerRepository);
    }

    @Bean
    public UpdateCustomer updateCustomer(CustomerRepository customerRepository) {
        return new UpdateCustomer(customerRepository);
    }

    @Bean
    public DeactivateCustomer deactivateCustomer(CustomerRepository customerRepository) {
        return new DeactivateCustomer(customerRepository);
    }

    @Bean
    public ReactivateCustomer reactivateCustomer(CustomerRepository customerRepository) {
        return new ReactivateCustomer(customerRepository);
    }

    @Bean
    public RegisterLocation registerLocation(
            CustomerRepository customerRepository,
            LocationRepository locationRepository
    ) {
        return new RegisterLocation(customerRepository, locationRepository);
    }

    @Bean
    public UpdateLocation updateLocation(LocationRepository locationRepository) {
        return new UpdateLocation(locationRepository);
    }

    @Bean
    public DeactivateLocation deactivateLocation(LocationRepository locationRepository) {
        return new DeactivateLocation(locationRepository);
    }

    @Bean
    public ReactivateLocation reactivateLocation(LocationRepository locationRepository) {
        return new ReactivateLocation(locationRepository);
    }

    @Bean
    public InstallEquipment installEquipment(
            InstalledEquipmentRepository installedEquipmentRepository,
            LocationRepository locationRepository,
            CustomerRepository customerRepository,
            EquipmentModelRepository equipmentModelRepository,
            Clock clock
    ) {
        return new InstallEquipment(
                installedEquipmentRepository,
                locationRepository,
                customerRepository,
                equipmentModelRepository,
                clock
        );
    }

    @Bean
    public UpdateInstalledEquipment updateInstalledEquipment(
            InstalledEquipmentRepository installedEquipmentRepository,
            Clock clock
    ) {
        return new UpdateInstalledEquipment(installedEquipmentRepository, clock);
    }

    @Bean
    public DeactivateInstalledEquipment deactivateInstalledEquipment(
            InstalledEquipmentRepository installedEquipmentRepository
    ) {
        return new DeactivateInstalledEquipment(installedEquipmentRepository);
    }

    @Bean
    public ReactivateInstalledEquipment reactivateInstalledEquipment(
            InstalledEquipmentRepository installedEquipmentRepository
    ) {
        return new ReactivateInstalledEquipment(installedEquipmentRepository);
    }

    // --- clientes: queries --------------------------------------------------------------------

    @Bean
    public ListCustomers listCustomers(CustomerReader customerReader) {
        return new ListCustomers(customerReader);
    }

    @Bean
    public GetCustomer getCustomer(CustomerReader customerReader) {
        return new GetCustomer(customerReader);
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
