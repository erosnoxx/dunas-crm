package com.crm.registry.infrastructure.customers.readers;

import com.crm.common.application.pages.Page;
import com.crm.common.application.pages.Pageable;
import com.crm.registry.application.customers.schemas.CustomerDetail;
import com.crm.registry.application.customers.schemas.CustomerFilter;
import com.crm.registry.application.customers.schemas.CustomerStatusFilter;
import com.crm.registry.application.customers.schemas.CustomerSummary;
import com.crm.registry.application.customers.schemas.CustomerSummary.CityView;
import com.crm.registry.application.customers.schemas.CustomerSummary.MatchedSerial;
import com.crm.registry.domain.catalog.entities.EquipmentModel;
import com.crm.registry.domain.catalog.entities.EquipmentType;
import com.crm.registry.domain.catalog.entities.Manufacturer;
import com.crm.registry.domain.catalog.enums.CatalogStatus;
import com.crm.registry.domain.catalog.enums.MeasurementUnit;
import com.crm.registry.domain.catalog.vos.CharacteristicAmount;
import com.crm.registry.domain.catalog.vos.CharacteristicName;
import com.crm.registry.domain.catalog.vos.CharacteristicValue;
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

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Reader da lista e da ficha de clientes contra o Postgres real (Testcontainers; exige Docker).
 * Os dados nascem pelos repositories, como os use cases gravam.
 */
@SpringBootTest(
        classes = CustomersReaderPostgresTest.TestApplication.class,
        properties = {
                "spring.flyway.locations=classpath:db/migration/registry",
                "spring.flyway.table=flyway_schema_history_registry",
                "spring.jpa.hibernate.ddl-auto=validate"
        }
)
@DisplayName("Reader de clientes contra o Postgres")
class CustomersReaderPostgresTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-21T12:00:00Z"), ZoneOffset.UTC);

    private static final String CPF_123 = "123.456.789-09";

    private static final String CPF = "529.982.247-25";

    private static final String CNPJ = "11.222.333/0001-81";

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

        @Bean
        CustomerReaderImpl customerReader(EntityManager em) {
            return new CustomerReaderImpl(em);
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
    private CustomerReaderImpl reader;

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

    // --- dados ------------------------------------------------------------------------------

    private Customer savedCustomer(
            String document,
            String name,
            String tradeName
    ) {
        return customers.save(Customer.register(
                Document.of(document),
                CustomerName.of(name),
                tradeName == null ? null : CustomerName.of(tradeName),
                Email.of("contato@exemplo.com"),
                null
        ));
    }

    private Customer savedCustomer(String document, String name) {
        return savedCustomer(document, name, null);
    }

    private Location savedLocation(
            Customer customer,
            String name,
            String city,
            UF state
    ) {
        var address = new Address(
                ZipCode.of("01310100"),
                Street.of("Avenida Paulista"),
                AddressNumber.of("1000"),
                null,
                Neighborhood.of("Bela Vista"),
                CityName.of(city),
                state
        );

        return locations.save(Location.register(customer, LocationName.of(name), address));
    }

    private Location savedLocation(Customer customer, String city) {
        return savedLocation(customer, "Local " + city, city, UF.SP);
    }

    private EquipmentType savedType(String name) {
        return types.save(EquipmentType.register(
                TypeName.of(name),
                CharacteristicName.of("Potência"),
                MeasurementUnit.KILOWATT,
                false
        ));
    }

    private EquipmentModel savedModel(
            EquipmentType type,
            String name,
            String amount
    ) {
        var manufacturer = manufacturers.save(Manufacturer.register(ManufacturerName.of("Fab " + name)));
        var values = amount == null
                ? List.<CharacteristicValue>of()
                : List.of(new CharacteristicValue(type.getCharacteristic().getId(), CharacteristicAmount.of(amount)));

        return models.save(EquipmentModel.register(manufacturer, type, ModelName.of(name), values));
    }

    private EquipmentModel savedModel(String name) {
        return savedModel(savedType("Tipo " + name), name, null);
    }

    private InstalledEquipment savedEquipment(
            Location location,
            Customer customer,
            EquipmentModel model,
            String serial
    ) {
        return equipment.save(InstalledEquipment.install(location, customer, model, SerialNumber.of(serial), null));
    }

    private void deactivate(Location location) {
        var loaded = locations.findById(location.getId()).orElseThrow();
        loaded.deactivate();
        locations.save(loaded);
    }

    private void deactivate(InstalledEquipment installed) {
        var loaded = equipment.findById(installed.getId()).orElseThrow();
        loaded.deactivate();
        equipment.save(loaded);
    }

    private void deactivate(Customer customer) {
        var loaded = customers.findById(customer.getId()).orElseThrow();
        loaded.deactivate();
        customers.save(loaded);
    }

    // --- consultas --------------------------------------------------------------------------

    private Page<CustomerSummary> search(String text) {
        return reader.search(new CustomerFilter(text, null), Pageable.of(0, 20));
    }

    private List<UUID> idsFound(String text) {
        return search(text).content().stream().map(CustomerSummary::id).toList();
    }

    private CustomerSummary only(String text) {
        var content = search(text).content();
        assertThat(content).hasSize(1);

        return content.getFirst();
    }

    // --- busca ------------------------------------------------------------------------------

    @Test
    @DisplayName("a busca acha pelo nome, nome fantasia, cidade, documento e série; % e _ são literais")
    void searchByNameTradeNameCityDocumentAndSerial() {
        var maria = savedCustomer(CPF_123, "Maria Silva");
        var model = savedModel("SUN2000");
        savedEquipment(savedLocation(maria, "Campinas"), maria, model, "HV22-001");
        var bakery = savedCustomer(CNPJ, "Padaria 100% Pão", "Loja_Sol");
        savedLocation(bakery, "Santos");
        var other = savedCustomer(CPF, "Padaria 1000 Pão");
        savedLocation(other, "Sorocaba");

        assertThat(idsFound("MARIA")).containsExactly(maria.getId());
        assertThat(idsFound("silva")).containsExactly(maria.getId());
        assertThat(idsFound("loja_")).containsExactly(bakery.getId());
        assertThat(idsFound("campinas")).containsExactly(maria.getId());
        assertThat(idsFound("123.456")).containsExactly(maria.getId());
        assertThat(idsFound("333/0001")).containsExactly(bakery.getId());
        assertThat(idsFound("hv22")).containsExactly(maria.getId());
        assertThat(idsFound("100%")).containsExactly(bakery.getId());
        assertThat(idsFound("%")).containsExactly(bakery.getId());
        assertThat(idsFound("_")).containsExactly(bakery.getId());
        assertThat(idsFound("padaria")).containsExactlyInAnyOrder(bakery.getId(), other.getId());
    }

    @Test
    @DisplayName("a busca ignora a cidade de um local desativado")
    void searchIgnoresTheCityOfADeactivatedLocation() {
        var maria = savedCustomer(CPF, "Maria Silva");
        var campinas = savedLocation(maria, "Campinas");
        savedLocation(maria, "Santos");

        assertThat(idsFound("campinas")).containsExactly(maria.getId());

        deactivate(campinas);

        assertThat(idsFound("campinas")).isEmpty();
        assertThat(idsFound("santos")).containsExactly(maria.getId());
    }

    @Test
    @DisplayName("a busca ignora a série de um equipamento desativado")
    void searchIgnoresTheSerialOfADeactivatedEquipment() {
        var maria = savedCustomer(CPF, "Maria Silva");
        var installed = savedEquipment(savedLocation(maria, "Campinas"), maria, savedModel("SUN2000"), "HV22-001");

        assertThat(idsFound("hv22")).containsExactly(maria.getId());

        deactivate(installed);

        assertThat(idsFound("hv22")).isEmpty();
    }

    @Test
    @DisplayName("a mesma série em modelos de clientes diferentes devolve os dois, cada um com o seu modelo")
    void sameSerialInTwoModelsOfTwoCustomersReturnsBoth() {
        var ana = savedCustomer(CPF, "Ana");
        var bruno = savedCustomer(CNPJ, "Bruno");
        savedEquipment(savedLocation(ana, "Campinas"), ana, savedModel("Modelo A"), "ABC-123");
        savedEquipment(savedLocation(bruno, "Santos"), bruno, savedModel("Modelo B"), "ABC-123");

        var content = search("abc-123").content();

        assertThat(content).extracting(CustomerSummary::id).containsExactly(ana.getId(), bruno.getId());
        assertThat(content.get(0).matchedSerials()).containsExactly(new MatchedSerial("ABC-123", "Modelo A"));
        assertThat(content.get(1).matchedSerials()).containsExactly(new MatchedSerial("ABC-123", "Modelo B"));
    }

    @Test
    @DisplayName("matchedSerials traz no máximo três séries ativas, em ordem de série, com o nome do modelo")
    void matchedSerialsHasAtMostThreeInSerialOrderWithTheModelName() {
        var maria = savedCustomer(CPF, "Maria Silva");
        var location = savedLocation(maria, "Campinas");
        var model = savedModel("SUN2000");
        savedEquipment(location, maria, model, "HV22-004");
        savedEquipment(location, maria, model, "HV22-002");
        savedEquipment(location, maria, model, "HV22-003");
        savedEquipment(location, maria, model, "XYZ-999");
        deactivate(savedEquipment(location, maria, model, "HV22-000"));
        savedEquipment(location, maria, savedModel("MOD-B"), "HV22-001");

        assertThat(only("hv22").matchedSerials()).containsExactly(
                new MatchedSerial("HV22-001", "MOD-B"),
                new MatchedSerial("HV22-002", "SUN2000"),
                new MatchedSerial("HV22-003", "SUN2000")
        );
    }

    @Test
    @DisplayName("matchedSerials vem vazia sem texto e quando o texto casou o nome")
    void matchedSerialsIsEmptyWithoutTextOrWhenTheTextMatchedTheName() {
        var maria = savedCustomer(CPF, "Maria Silva");
        savedEquipment(savedLocation(maria, "Campinas"), maria, savedModel("SUN2000"), "HV22-001");

        assertThat(only("hv22").matchedSerials()).isNotEmpty();
        assertThat(only(null).matchedSerials()).isEmpty();
        assertThat(only("  ").matchedSerials()).isEmpty();
        assertThat(only("silva").matchedSerials()).isEmpty();
    }

    @Test
    @DisplayName("um cliente com muitos locais e equipamentos é uma linha só, e o total da página está certo")
    void aCustomerWithManyLocationsIsOneRowAndTheTotalIsRight() {
        var ana = savedCustomer(CPF, "Ana");
        var model = savedModel("SUN2000");

        for (var i = 0; i < 5; i++) {
            var location = savedLocation(ana, "Cidade " + i);
            savedEquipment(location, ana, model, "HV-" + i + "-A");
            savedEquipment(location, ana, model, "HV-" + i + "-B");
        }

        var bruno = savedCustomer(CNPJ, "Bruno");
        var carla = savedCustomer(CPF_123, "Carla");

        var first = reader.search(new CustomerFilter(null, null), Pageable.of(0, 2));
        var second = reader.search(new CustomerFilter(null, null), Pageable.of(1, 2));
        var bySerial = reader.search(new CustomerFilter("hv", null), Pageable.of(0, 2));
        var byCity = reader.search(new CustomerFilter("cidade", null), Pageable.of(0, 2));

        assertThat(first.content()).extracting(CustomerSummary::id).containsExactly(ana.getId(), bruno.getId());
        assertThat(first.totalElements()).isEqualTo(3);
        assertThat(first.totalPages()).isEqualTo(2);
        assertThat(second.content()).extracting(CustomerSummary::id).containsExactly(carla.getId());
        assertThat(bySerial.content()).extracting(CustomerSummary::id).containsExactly(ana.getId());
        assertThat(bySerial.totalElements()).isEqualTo(1);
        assertThat(byCity.content()).extracting(CustomerSummary::id).containsExactly(ana.getId());
        assertThat(byCity.totalElements()).isEqualTo(1);
        assertThat(first.content().getFirst().locationCount()).isEqualTo(5);
        assertThat(first.content().getFirst().equipmentCount()).isEqualTo(10);
    }

    @Test
    @DisplayName("conta só locais ativos e equipamentos ativos em locais ativos")
    void countsOnlyActiveLocationsAndActiveEquipmentInActiveLocations() {
        var maria = savedCustomer(CPF, "Maria Silva");
        var model = savedModel("SUN2000");
        var active = savedLocation(maria, "Campinas");
        var closed = savedLocation(maria, "Santos");
        savedEquipment(active, maria, model, "A-1");
        savedEquipment(active, maria, model, "A-2");
        deactivate(savedEquipment(active, maria, model, "A-3"));
        savedEquipment(closed, maria, model, "B-1");
        deactivate(closed);
        savedCustomer(CNPJ, "Zeca");

        var content = search(null).content();

        assertThat(content.get(0).locationCount()).isEqualTo(1);
        assertThat(content.get(0).equipmentCount()).isEqualTo(2);
        assertThat(content.get(1).locationCount()).isZero();
        assertThat(content.get(1).equipmentCount()).isZero();
    }

    @Test
    @DisplayName("as cidades são distintas, só de locais ativos, em ordem de cidade")
    void citiesAreDistinctAndOnlyFromActiveLocations() {
        var maria = savedCustomer(CPF, "Maria Silva");
        savedLocation(maria, "Casa", "Santos", UF.SP);
        savedLocation(maria, "Loja", "Campinas", UF.SP);
        savedLocation(maria, "Galpão", "Santos", UF.SP);
        deactivate(savedLocation(maria, "Praia", "Recife", UF.PE));

        var summary = only(null);

        assertThat(summary.cities()).containsExactly(
                new CityView("Campinas", UF.SP),
                new CityView("Santos", UF.SP)
        );
        assertThat(summary.document()).isEqualTo("52998224725");
        assertThat(summary.documentType()).isEqualTo(Document.DocumentType.CPF);
        assertThat(summary.status()).isEqualTo(CustomerStatus.ACTIVE);
    }

    @Test
    @DisplayName("sem status a lista mostra os ativos; DEACTIVATED os desativados; ALL os dois")
    void statusFilterAllAndDefault() {
        var active = savedCustomer(CPF, "Ana");
        var deactivated = savedCustomer(CNPJ, "Bruno", "Bruno Fantasia");
        deactivate(deactivated);

        var byDefault = reader.search(new CustomerFilter(null, null), Pageable.of(0, 20));
        var onlyDeactivated = reader.search(new CustomerFilter(null, CustomerStatusFilter.DEACTIVATED), Pageable.of(0, 20));
        var all = reader.search(new CustomerFilter(null, CustomerStatusFilter.ALL), Pageable.of(0, 20));

        assertThat(byDefault.content()).extracting(CustomerSummary::id).containsExactly(active.getId());
        assertThat(onlyDeactivated.content()).extracting(CustomerSummary::id).containsExactly(deactivated.getId());
        assertThat(onlyDeactivated.content().getFirst().documentType()).isEqualTo(Document.DocumentType.CNPJ);
        assertThat(onlyDeactivated.content().getFirst().tradeName()).isEqualTo("Bruno Fantasia");
        assertThat(all.content()).extracting(CustomerSummary::id).containsExactly(active.getId(), deactivated.getId());
        assertThat(all.totalElements()).isEqualTo(2);
    }

    // --- ficha ------------------------------------------------------------------------------

    @Test
    @DisplayName("a ficha traz locais e equipamentos desativados, na ordem da spec")
    void detailBringsDeactivatedLocationsAndEquipmentInTheSpecOrder() {
        var maria = savedCustomer(CNPJ, "Maria Silva", "Solar Maria");
        var beta = savedLocation(maria, "beta", "Campinas", UF.SP);
        var casa = savedLocation(maria, "Casa", "Santos", UF.SP);
        var alfa = savedLocation(maria, "Alfa", "Recife", UF.PE);

        var inverter = savedType("Inversor");
        var battery = savedType("bateria");
        var sun = savedModel(inverter, "SUN2000", "5.5");
        var aaa = savedModel(inverter, "aaa", null);
        var pack = savedModel(battery, "Pack", "10");

        var dated = equipment.save(InstalledEquipment.install(
                beta,
                maria,
                sun,
                SerialNumber.of("B"),
                InstallationDate.of(LocalDate.parse("2026-11-01"), CLOCK)
        ));
        var sunA = savedEquipment(beta, maria, sun, "A");
        var aaaZ = savedEquipment(beta, maria, aaa, "Z");
        var packC = savedEquipment(beta, maria, pack, "C");
        var gone = savedEquipment(beta, maria, pack, "0");
        deactivate(gone);
        var inAlfa = savedEquipment(alfa, maria, sun, "X");
        deactivate(alfa);

        var detail = reader.findDetail(maria.getId()).orElseThrow();

        assertThat(detail.id()).isEqualTo(maria.getId());
        assertThat(detail.document()).isEqualTo("11222333000181");
        assertThat(detail.documentType()).isEqualTo(Document.DocumentType.CNPJ);
        assertThat(detail.name()).isEqualTo("Maria Silva");
        assertThat(detail.tradeName()).isEqualTo("Solar Maria");
        assertThat(detail.email()).isEqualTo("contato@exemplo.com");
        assertThat(detail.phone()).isNull();
        assertThat(detail.status()).isEqualTo(CustomerStatus.ACTIVE);

        assertThat(detail.locations()).extracting(CustomerDetail.LocationView::id)
                .containsExactly(beta.getId(), casa.getId(), alfa.getId());
        assertThat(detail.locations().get(2).status()).isEqualTo(CustomerStatus.DEACTIVATED);
        assertThat(detail.locations().get(0).address()).isEqualTo(new CustomerDetail.AddressView(
                "01310100",
                "Avenida Paulista",
                "1000",
                null,
                "Bela Vista",
                "Campinas",
                UF.SP
        ));

        var inBeta = detail.locations().get(0).equipment();

        assertThat(inBeta).extracting(CustomerDetail.EquipmentView::id)
                .containsExactly(packC.getId(), aaaZ.getId(), sunA.getId(), dated.getId(), gone.getId());
        assertThat(inBeta.get(4).status()).isEqualTo(CustomerStatus.DEACTIVATED);
        assertThat(detail.locations().get(1).equipment()).isEmpty();
        assertThat(detail.locations().get(2).equipment()).extracting(CustomerDetail.EquipmentView::id)
                .containsExactly(inAlfa.getId());

        var full = inBeta.get(3);

        assertThat(full.serialNumber()).isEqualTo("B");
        assertThat(full.installedOn()).isEqualTo(LocalDate.parse("2026-11-01"));
        assertThat(full.status()).isEqualTo(CustomerStatus.ACTIVE);
        assertThat(full.model()).isEqualTo(new CustomerDetail.ModelRef(sun.getId(), "SUN2000", CatalogStatus.ACTIVE));
        assertThat(full.manufacturer()).isEqualTo(new CustomerDetail.Ref(sun.getManufacturerId(), "Fab SUN2000"));
        assertThat(full.type()).isEqualTo(new CustomerDetail.Ref(inverter.getId(), "Inversor"));
        assertThat(full.characteristic().name()).isEqualTo("Potência");
        assertThat(full.characteristic().unit()).isEqualTo(MeasurementUnit.KILOWATT);
        assertThat(full.characteristic().amount()).isEqualByComparingTo(new BigDecimal("5.5"));
        assertThat(inBeta.get(0).installedOn()).isNull();
    }

    @Test
    @DisplayName("na ficha, o equipamento de um modelo sem valor vem sem característica")
    void detailOfAModelWithoutValueHasNoCharacteristic() {
        var maria = savedCustomer(CPF, "Maria Silva");
        savedEquipment(savedLocation(maria, "Campinas"), maria, savedModel("SUN2000"), "HV22-001");

        var installed = reader.findDetail(maria.getId()).orElseThrow().locations().getFirst().equipment().getFirst();

        assertThat(installed.serialNumber()).isEqualTo("HV22-001");
        assertThat(installed.characteristic()).isNull();
    }

    @Test
    @DisplayName("a ficha de um cliente que não existe é vazia")
    void detailNotFoundIsEmpty() {
        savedCustomer(CPF, "Maria Silva");

        assertThat(reader.findDetail(UUID.randomUUID())).isEmpty();
    }
}
