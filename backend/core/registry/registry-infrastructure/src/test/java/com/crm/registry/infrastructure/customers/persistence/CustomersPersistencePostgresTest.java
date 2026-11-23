package com.crm.registry.infrastructure.customers.persistence;

import com.crm.common.application.exceptions.ConcurrentUpdateException;
import com.crm.common.application.exceptions.ConflictException;
import com.crm.registry.domain.catalog.entities.EquipmentModel;
import com.crm.registry.domain.catalog.entities.EquipmentType;
import com.crm.registry.domain.catalog.entities.Manufacturer;
import com.crm.registry.domain.catalog.enums.MeasurementUnit;
import com.crm.registry.domain.catalog.vos.CharacteristicName;
import com.crm.registry.domain.catalog.vos.ManufacturerName;
import com.crm.registry.domain.catalog.vos.ModelName;
import com.crm.registry.domain.catalog.vos.TypeName;
import com.crm.registry.domain.customers.entities.Customer;
import com.crm.registry.domain.customers.entities.InstalledEquipment;
import com.crm.registry.domain.customers.entities.Location;
import com.crm.registry.domain.customers.enums.CustomerStatus;
import com.crm.registry.domain.customers.vos.CustomerName;
import com.crm.registry.domain.customers.vos.InstallationDate;
import com.crm.registry.domain.customers.vos.LocationName;
import com.crm.registry.domain.customers.vos.SerialNumber;
import com.crm.registry.infrastructure.catalog.mappers.EquipmentModelMapper;
import com.crm.registry.infrastructure.catalog.mappers.EquipmentTypeMapper;
import com.crm.registry.infrastructure.catalog.mappers.ManufacturerMapper;
import com.crm.registry.infrastructure.catalog.persistence.entities.ManufacturerEntity;
import com.crm.registry.infrastructure.catalog.persistence.jpa.EquipmentModelJpaRepository;
import com.crm.registry.infrastructure.catalog.persistence.jpa.EquipmentTypeJpaRepository;
import com.crm.registry.infrastructure.catalog.persistence.jpa.ManufacturerJpaRepository;
import com.crm.registry.infrastructure.catalog.repositories.EquipmentModelRepositoryImpl;
import com.crm.registry.infrastructure.catalog.repositories.EquipmentTypeRepositoryImpl;
import com.crm.registry.infrastructure.catalog.repositories.ManufacturerRepositoryImpl;
import com.crm.registry.infrastructure.customers.mappers.CustomerMapper;
import com.crm.registry.infrastructure.customers.mappers.InstalledEquipmentMapper;
import com.crm.registry.infrastructure.customers.mappers.LocationMapper;
import com.crm.registry.infrastructure.customers.persistence.entities.CustomerEntity;
import com.crm.registry.infrastructure.customers.persistence.jpa.CustomerJpaRepository;
import com.crm.registry.infrastructure.customers.persistence.jpa.InstalledEquipmentJpaRepository;
import com.crm.registry.infrastructure.customers.persistence.jpa.LocationJpaRepository;
import com.crm.registry.infrastructure.customers.repositories.CustomerRepositoryImpl;
import com.crm.registry.infrastructure.customers.repositories.InstalledEquipmentRepositoryImpl;
import com.crm.registry.infrastructure.customers.repositories.LocationRepositoryImpl;
import com.crm.shared.domain.enums.UF;
import com.crm.shared.domain.vos.address.Address;
import com.crm.shared.domain.vos.address.AddressNumber;
import com.crm.shared.domain.vos.address.CityName;
import com.crm.shared.domain.vos.address.Neighborhood;
import com.crm.shared.domain.vos.address.Street;
import com.crm.shared.domain.vos.address.ZipCode;
import com.crm.shared.domain.vos.contact.Email;
import com.crm.shared.domain.vos.identity.Document;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Persistência de clientes contra o Postgres real (Testcontainers; exige Docker). Sobe com
 * {@code ddl-auto=validate}: se a migration e as entities divergirem, o contexto nem carrega.
 */
@SpringBootTest(
        classes = CustomersPersistencePostgresTest.TestApplication.class,
        properties = {
                "spring.flyway.locations=classpath:db/migration/registry",
                "spring.flyway.table=flyway_schema_history_registry",
                "spring.jpa.hibernate.ddl-auto=validate"
        }
)
@DisplayName("Persistência de clientes contra o Postgres")
class CustomersPersistencePostgresTest {

    private static final Instant NOW = Instant.parse("2026-11-21T12:00:00Z");

    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    private static final String CPF = "529.982.247-25";

    private static final String CPF_RAW = "52998224725";

    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16");

    static {
        POSTGRES.start();
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EntityScan(basePackageClasses = {ManufacturerEntity.class, CustomerEntity.class})
    @EnableJpaRepositories(basePackageClasses = {ManufacturerJpaRepository.class, CustomerJpaRepository.class})
    static class TestApplication {

        @Bean
        Clock clock() {
            return CLOCK;
        }

        @Bean
        ManufacturerRepositoryImpl manufacturerRepository(ManufacturerJpaRepository jpa, EntityManager em) {
            return new ManufacturerRepositoryImpl(jpa, em, new ManufacturerMapper());
        }

        @Bean
        EquipmentTypeRepositoryImpl typeRepository(EquipmentTypeJpaRepository jpa, EntityManager em) {
            return new EquipmentTypeRepositoryImpl(jpa, em, new EquipmentTypeMapper());
        }

        @Bean
        EquipmentModelRepositoryImpl modelRepository(EquipmentModelJpaRepository jpa, EntityManager em) {
            return new EquipmentModelRepositoryImpl(jpa, em, new EquipmentModelMapper());
        }

        @Bean
        CustomerRepositoryImpl customerRepository(CustomerJpaRepository jpa, EntityManager em) {
            return new CustomerRepositoryImpl(jpa, em, new CustomerMapper());
        }

        @Bean
        LocationRepositoryImpl locationRepository(LocationJpaRepository jpa, EntityManager em) {
            return new LocationRepositoryImpl(jpa, em, new LocationMapper());
        }

        @Bean
        InstalledEquipmentRepositoryImpl installedEquipmentRepository(InstalledEquipmentJpaRepository jpa, EntityManager em) {
            return new InstalledEquipmentRepositoryImpl(jpa, em, new InstalledEquipmentMapper());
        }
    }

    @Autowired
    private ManufacturerRepositoryImpl manufacturers;

    @Autowired
    private EquipmentTypeRepositoryImpl types;

    @Autowired
    private EquipmentModelRepositoryImpl models;

    @Autowired
    private CustomerRepositoryImpl customers;

    @Autowired
    private LocationRepositoryImpl locations;

    @Autowired
    private InstalledEquipmentRepositoryImpl equipment;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void cleanTables() {
        jdbc.execute("DELETE FROM installed_equipment");
        jdbc.execute("DELETE FROM locations");
        jdbc.execute("DELETE FROM customers");
        jdbc.execute("DELETE FROM model_characteristic_values");
        jdbc.execute("DELETE FROM equipment_models");
        jdbc.execute("DELETE FROM characteristic_definitions");
        jdbc.execute("DELETE FROM equipment_types");
        jdbc.execute("DELETE FROM manufacturers");
    }

    private Customer savedCustomer(String document) {
        return customers.save(Customer.register(
                Document.of(document),
                CustomerName.of("Maria Silva"),
                null,
                Email.of("maria@exemplo.com"),
                null
        ));
    }

    private static Address address() {
        return new Address(
                ZipCode.of("01310100"),
                Street.of("Avenida Paulista"),
                AddressNumber.of("1000"),
                null,
                Neighborhood.of("Bela Vista"),
                CityName.of("São Paulo"),
                UF.SP
        );
    }

    private Location savedLocation(Customer customer) {
        return locations.save(Location.register(customer, LocationName.of("Casa"), address()));
    }

    private EquipmentModel savedModel(String name) {
        var manufacturer = manufacturers.save(Manufacturer.register(ManufacturerName.of("Fab " + name)));
        var type = types.save(EquipmentType.register(
                TypeName.of("Tipo " + name),
                CharacteristicName.of("Potência"),
                MeasurementUnit.KILOWATT,
                false
        ));

        return models.save(EquipmentModel.register(manufacturer, type, ModelName.of(name), List.of()));
    }

    private InstalledEquipment savedEquipment(
            Location location,
            Customer customer,
            EquipmentModel model,
            String serial
    ) {
        return equipment.save(InstalledEquipment.install(location, customer, model, SerialNumber.of(serial), null));
    }

    private static String codeOf(Throwable error) {
        return ((ConflictException) error).getCode().orElse(null);
    }

    @Test
    @DisplayName("grava e lê os três aggregates; versão 0 ao gravar, editar sobe para 1")
    void savesAndLoadsTheThreeAggregates() {
        var customer = savedCustomer(CPF);
        var location = savedLocation(customer);
        var model = savedModel("SUN2000");
        var date = InstallationDate.of(LocalDate.parse("2026-11-01"), CLOCK);
        var installed = equipment.save(InstalledEquipment.install(location, customer, model, SerialNumber.of("SN-1"), date));

        assertThat(customer.getVersion()).contains(0L);
        assertThat(location.getVersion()).contains(0L);
        assertThat(installed.getVersion()).contains(0L);

        var loadedCustomer = customers.findById(customer.getId()).orElseThrow();
        var loadedLocation = locations.findById(location.getId()).orElseThrow();
        var loadedEquipment = equipment.findById(installed.getId()).orElseThrow();

        assertThat(loadedCustomer.getDocument()).isEqualTo(Document.of(CPF_RAW));
        assertThat(loadedCustomer.getEmail()).isPresent();
        assertThat(loadedCustomer.getPhone()).isEmpty();
        assertThat(loadedLocation.getCustomerId()).isEqualTo(customer.getId());
        assertThat(loadedLocation.getAddress()).isEqualTo(address());
        assertThat(loadedEquipment.getModelId()).isEqualTo(model.getId());
        assertThat(loadedEquipment.getInstalledOn()).contains(date);
        assertThat(loadedEquipment.getStatus()).isEqualTo(CustomerStatus.ACTIVE);

        loadedCustomer.edit(CustomerName.of("Maria Souza"), null, Email.of("maria@exemplo.com"), null);
        loadedLocation.edit(LocationName.of("Casa de praia"), address());
        loadedEquipment.edit(SerialNumber.of("SN-2"), null);

        assertThat(customers.save(loadedCustomer).getVersion()).contains(1L);
        assertThat(locations.save(loadedLocation).getVersion()).contains(1L);
        assertThat(equipment.save(loadedEquipment).getVersion()).contains(1L);
        assertThat(equipment.findById(installed.getId()).orElseThrow().getInstalledOn()).isEmpty();
    }

    @Test
    @DisplayName("o mesmo documento em outra formatação é ConflictException com o código do documento")
    void sameDocumentInAnotherFormattingIsAConflictWithItsCode() {
        savedCustomer(CPF);

        assertThatThrownBy(() -> savedCustomer(CPF_RAW))
                .isInstanceOf(ConflictException.class)
                .hasMessage("já existe um cliente com este documento")
                .satisfies(error -> assertThat(codeOf(error)).isEqualTo("customer_document_taken"));
        assertThat(customers.existsByDocument(Document.of(CPF))).isTrue();
        assertThat(customers.existsByDocument(Document.of("11.222.333/0001-81"))).isFalse();
    }

    @Test
    @DisplayName("dois cadastros do mesmo documento ao mesmo tempo: um grava, o outro recebe ConflictException com o código")
    void twoRegistrationsOfTheSameDocumentAtTheSameTimeLeaveOneAndConflictTheOther() throws Exception {
        var failures = race(
                () -> savedCustomer(CPF),
                () -> savedCustomer(CPF_RAW)
        );

        assertThat(failures).singleElement()
                .isInstanceOf(ConflictException.class)
                .satisfies(error -> assertThat(codeOf(error)).isEqualTo("customer_document_taken"));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM customers", Long.class)).isEqualTo(1L);
    }

    @Test
    @DisplayName("a mesma série em outra caixa para o mesmo modelo é ConflictException com o código da série")
    void sameSerialInAnotherCaseForTheSameModelIsAConflict() {
        var customer = savedCustomer(CPF);
        var location = savedLocation(customer);
        var model = savedModel("SUN2000");

        savedEquipment(location, customer, model, "ABC-123");

        assertThatThrownBy(() -> savedEquipment(location, customer, model, "abc-123"))
                .isInstanceOf(ConflictException.class)
                .hasMessage("este modelo já tem um equipamento instalado com esse número de série")
                .satisfies(error -> assertThat(codeOf(error)).isEqualTo("serial_number_taken"));
    }

    @Test
    @DisplayName("duas instalações da mesma série ao mesmo tempo: uma grava, a outra recebe ConflictException com o código")
    void twoInstallationsOfTheSameSerialAtTheSameTimeLeaveOneAndConflictTheOther() throws Exception {
        var customer = savedCustomer(CPF);
        var location = savedLocation(customer);
        var model = savedModel("SUN2000");

        var failures = race(
                () -> savedEquipment(location, customer, model, "ABC-123"),
                () -> savedEquipment(location, customer, model, "abc-123")
        );

        assertThat(failures).singleElement()
                .isInstanceOf(ConflictException.class)
                .satisfies(error -> assertThat(codeOf(error)).isEqualTo("serial_number_taken"));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM installed_equipment", Long.class)).isEqualTo(1L);
    }

    @Test
    @DisplayName("a mesma série vale para outro modelo")
    void sameSerialForAnotherModelIsAllowed() {
        var customer = savedCustomer(CPF);
        var location = savedLocation(customer);

        savedEquipment(location, customer, savedModel("Modelo A"), "ABC-123");
        savedEquipment(location, customer, savedModel("Modelo B"), "ABC-123");

        assertThat(jdbc.queryForObject("SELECT count(*) FROM installed_equipment", Long.class)).isEqualTo(2L);
    }

    @Test
    @DisplayName("existsByModelAndSerialNumber com exceptId nulo funciona no Postgres e ignora a caixa")
    void existsByModelAndSerialNumberWithNullExceptIdWorksOnPostgres() {
        var customer = savedCustomer(CPF);
        var location = savedLocation(customer);
        var model = savedModel("SUN2000");

        savedEquipment(location, customer, model, "ABC-123");

        assertThat(equipment.existsByModelAndSerialNumber(model.getId(), SerialNumber.of("abc-123"), null)).isTrue();
        assertThat(equipment.existsByModelAndSerialNumber(model.getId(), SerialNumber.of("OUTRA"), null)).isFalse();
        assertThat(equipment.existsByModelAndSerialNumber(UUID.randomUUID(), SerialNumber.of("ABC-123"), null)).isFalse();
    }

    @Test
    @DisplayName("existsByModelAndSerialNumber ignora o id excetuado")
    void existsByModelAndSerialNumberIgnoresTheExceptedId() {
        var customer = savedCustomer(CPF);
        var location = savedLocation(customer);
        var model = savedModel("SUN2000");
        var installed = savedEquipment(location, customer, model, "ABC-123");

        assertThat(equipment.existsByModelAndSerialNumber(model.getId(), SerialNumber.of("ABC-123"), installed.getId())).isFalse();
        assertThat(equipment.existsByModelAndSerialNumber(model.getId(), SerialNumber.of("ABC-123"), UUID.randomUUID())).isTrue();
    }

    @Test
    @DisplayName("editar um cliente a partir de uma cópia velha é ConcurrentUpdateException e não sobrescreve a edição gravada")
    void staleEditOfACustomerIsAConcurrentUpdate() {
        var saved = savedCustomer(CPF);
        var first = customers.findById(saved.getId()).orElseThrow();
        var second = customers.findById(saved.getId()).orElseThrow();

        second.edit(CustomerName.of("Segunda Edição"), null, Email.of("maria@exemplo.com"), null);
        customers.save(second);

        first.edit(CustomerName.of("Primeira Edição"), null, Email.of("maria@exemplo.com"), null);

        assertThatThrownBy(() -> customers.save(first)).isInstanceOf(ConcurrentUpdateException.class);
        assertThat(customers.findById(saved.getId()).orElseThrow().getName()).isEqualTo(CustomerName.of("Segunda Edição"));
    }

    @Test
    @DisplayName("editar um local a partir de uma cópia velha é ConcurrentUpdateException e não sobrescreve a edição gravada")
    void staleEditOfALocationIsAConcurrentUpdate() {
        var saved = savedLocation(savedCustomer(CPF));
        var first = locations.findById(saved.getId()).orElseThrow();
        var second = locations.findById(saved.getId()).orElseThrow();

        second.edit(LocationName.of("Segundo"), address());
        locations.save(second);

        first.edit(LocationName.of("Primeiro"), address());

        assertThatThrownBy(() -> locations.save(first)).isInstanceOf(ConcurrentUpdateException.class);
        assertThat(locations.findById(saved.getId()).orElseThrow().getName()).isEqualTo(LocationName.of("Segundo"));
    }

    @Test
    @DisplayName("editar um equipamento instalado a partir de uma cópia velha é ConcurrentUpdateException e não sobrescreve a edição gravada")
    void staleEditOfAnInstalledEquipmentIsAConcurrentUpdate() {
        var customer = savedCustomer(CPF);
        var saved = savedEquipment(savedLocation(customer), customer, savedModel("SUN2000"), "ABC-123");
        var first = equipment.findById(saved.getId()).orElseThrow();
        var second = equipment.findById(saved.getId()).orElseThrow();

        second.edit(SerialNumber.of("SEGUNDO"), null);
        equipment.save(second);

        first.edit(SerialNumber.of("PRIMEIRO"), null);

        assertThatThrownBy(() -> equipment.save(first)).isInstanceOf(ConcurrentUpdateException.class);
        assertThat(equipment.findById(saved.getId()).orElseThrow().getSerialNumber()).isEqualTo(SerialNumber.of("SEGUNDO"));
    }

    @Test
    @DisplayName("cliente e equipamento desativados continuam segurando o documento e a série")
    void deactivatedRowsStillHoldTheDocumentAndTheSerial() {
        var customer = savedCustomer(CPF);
        var location = savedLocation(customer);
        var model = savedModel("SUN2000");
        var installed = savedEquipment(location, customer, model, "ABC-123");

        installed.deactivate();
        equipment.save(installed);
        customer.deactivate();
        customers.save(customer);

        assertThat(customers.existsByDocument(Document.of(CPF))).isTrue();
        assertThat(equipment.existsByModelAndSerialNumber(model.getId(), SerialNumber.of("ABC-123"), null)).isTrue();
        assertThatThrownBy(() -> savedCustomer(CPF_RAW))
                .satisfies(error -> assertThat(codeOf(error)).isEqualTo("customer_document_taken"));
        var again = InstalledEquipment.reconstitute(
                UUID.randomUUID(),
                null,
                location.getId(),
                model.getId(),
                SerialNumber.of("abc-123"),
                null,
                CustomerStatus.ACTIVE
        );

        assertThatThrownBy(() -> equipment.save(again))
                .satisfies(error -> assertThat(codeOf(error)).isEqualTo("serial_number_taken"));
    }

    /** Dispara as duas tarefas juntas e devolve as falhas; exige exatamente um vencedor. */
    private static List<Throwable> race(Callable<?> one, Callable<?> other) throws Exception {
        var ready = new CountDownLatch(2);
        var go = new CountDownLatch(1);
        var pool = Executors.newFixedThreadPool(2);

        try {
            var futures = new ArrayList<Future<?>>();

            for (var task : List.of(one, other))
                futures.add(pool.submit(() -> {
                    ready.countDown();
                    go.await();

                    return task.call();
                }));

            ready.await();
            go.countDown();

            var winners = 0;
            var failures = new ArrayList<Throwable>();

            for (var future : futures) {
                try {
                    future.get();
                    winners++;
                }
                catch (ExecutionException e) {
                    failures.add(e.getCause());
                }
            }

            assertThat(winners).isEqualTo(1);

            return failures;
        }
        finally {
            pool.shutdownNow();
        }
    }
}
