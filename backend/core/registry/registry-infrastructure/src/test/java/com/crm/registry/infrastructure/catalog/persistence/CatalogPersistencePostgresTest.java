package com.crm.registry.infrastructure.catalog.persistence;

import com.crm.common.application.exceptions.ConflictException;
import com.crm.common.application.pages.Pageable;
import com.crm.registry.application.catalog.schemas.EquipmentModelFilter;
import com.crm.registry.application.catalog.schemas.EquipmentModelSummary;
import com.crm.registry.application.catalog.schemas.EquipmentTypeSummary;
import com.crm.registry.application.catalog.schemas.ManufacturerFilter;
import com.crm.registry.application.catalog.schemas.ManufacturerSummary;
import com.crm.registry.application.catalog.schemas.StatusFilter;
import com.crm.registry.domain.catalog.entities.EquipmentModel;
import com.crm.registry.domain.catalog.entities.EquipmentType;
import com.crm.registry.domain.catalog.entities.Manufacturer;
import com.crm.registry.domain.catalog.vos.CatalogStatus;
import com.crm.registry.domain.catalog.vos.CharacteristicAmount;
import com.crm.registry.domain.catalog.vos.CharacteristicName;
import com.crm.registry.domain.catalog.vos.CharacteristicValue;
import com.crm.registry.domain.catalog.vos.ManufacturerName;
import com.crm.registry.domain.catalog.vos.MeasurementUnit;
import com.crm.registry.domain.catalog.vos.ModelName;
import com.crm.registry.domain.catalog.vos.TypeName;
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
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Persistência do catálogo contra o Postgres real (Testcontainers; exige Docker). Sobe com
 * {@code ddl-auto=validate}: se a migration e as entities divergirem, o contexto nem carrega.
 */
@SpringBootTest(
        classes = CatalogPersistencePostgresTest.TestApplication.class,
        properties = {
                "spring.flyway.locations=classpath:db/migration/registry",
                "spring.flyway.table=flyway_schema_history_registry",
                "spring.jpa.hibernate.ddl-auto=validate"
        }
)
@DisplayName("Persistência do catálogo contra o Postgres")
class CatalogPersistencePostgresTest {

    private static final Instant NOW = Instant.parse("2026-11-21T12:00:00Z");

    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16");

    static {
        POSTGRES.start();
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EntityScan(basePackageClasses = ManufacturerEntity.class)
    @EnableJpaRepositories(basePackageClasses = ManufacturerJpaRepository.class)
    static class TestApplication {

        @Bean
        Clock clock() {
            return Clock.fixed(NOW, ZoneOffset.UTC);
        }

        @Bean
        JpaManufacturerRepositoryImpl manufacturerRepository(ManufacturerJpaRepository jpa, EntityManager em) {
            return new JpaManufacturerRepositoryImpl(jpa, em, new ManufacturerMapper());
        }

        @Bean
        JpaEquipmentTypeRepositoryImpl typeRepository(EquipmentTypeJpaRepository jpa, EntityManager em) {
            return new JpaEquipmentTypeRepositoryImpl(jpa, em, new EquipmentTypeMapper());
        }

        @Bean
        JpaEquipmentModelRepositoryImpl modelRepository(EquipmentModelJpaRepository jpa, EntityManager em) {
            return new JpaEquipmentModelRepositoryImpl(jpa, em, new EquipmentModelMapper());
        }

        @Bean
        JpaManufacturerReaderImpl manufacturerReader(EntityManager em) {
            return new JpaManufacturerReaderImpl(em);
        }

        @Bean
        JpaEquipmentTypeReaderImpl typeReader(EntityManager em) {
            return new JpaEquipmentTypeReaderImpl(em);
        }

        @Bean
        JpaEquipmentModelReaderImpl modelReader(EntityManager em) {
            return new JpaEquipmentModelReaderImpl(em);
        }
    }

    @Autowired
    private JpaManufacturerRepositoryImpl manufacturers;

    @Autowired
    private JpaEquipmentTypeRepositoryImpl types;

    @Autowired
    private JpaEquipmentModelRepositoryImpl models;

    @Autowired
    private JpaManufacturerReaderImpl manufacturerReader;

    @Autowired
    private JpaEquipmentTypeReaderImpl typeReader;

    @Autowired
    private JpaEquipmentModelReaderImpl modelReader;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void cleanTables() {
        jdbc.execute("DELETE FROM model_characteristic_values");
        jdbc.execute("DELETE FROM equipment_models");
        jdbc.execute("DELETE FROM characteristic_definitions");
        jdbc.execute("DELETE FROM equipment_types");
        jdbc.execute("DELETE FROM manufacturers");
    }

    private Manufacturer savedManufacturer(String name) {
        return manufacturers.save(Manufacturer.register(ManufacturerName.of(name)));
    }

    private EquipmentType savedType(String name, MeasurementUnit unit) {
        return types.save(EquipmentType.register(
                TypeName.of(name),
                CharacteristicName.of("Potência"),
                unit,
                false
        ));
    }

    private EquipmentModel savedModel(
            Manufacturer manufacturer,
            EquipmentType type,
            String name,
            String amount
    ) {
        var values = amount == null
                ? List.<CharacteristicValue>of()
                : List.of(new CharacteristicValue(type.getCharacteristic().getId(), CharacteristicAmount.of(amount)));

        return models.save(EquipmentModel.register(
                manufacturer,
                type,
                ModelName.of(name),
                values
        ));
    }

    private static String codeOf(Throwable error) {
        return ((ConflictException) error).getCode().orElse(null);
    }

    @Test
    @DisplayName("grava e lê o modelo com o valor; editar troca o valor e sobe a versão")
    void savesAndLoadsAModelWithItsValue() {
        var huawei = savedManufacturer("Huawei");
        var inverter = savedType("Inversor", MeasurementUnit.KILOWATT);
        var saved = savedModel(huawei, inverter, "SUN2000-5KTL", "5.5");

        assertThat(saved.getVersion()).contains(0L);

        var loaded = models.findById(saved.getId()).orElseThrow();

        assertThat(loaded.getManufacturerId()).isEqualTo(huawei.getId());
        assertThat(loaded.getTypeId()).isEqualTo(inverter.getId());
        assertThat(loaded.getName()).isEqualTo(ModelName.of("SUN2000-5KTL"));
        assertThat(loaded.getStatus()).isEqualTo(CatalogStatus.ACTIVE);
        assertThat(loaded.getValues()).containsExactly(
                new CharacteristicValue(inverter.getCharacteristic().getId(), CharacteristicAmount.of("5.5"))
        );

        loaded.edit(
                inverter,
                ModelName.of("SUN2000-6KTL"),
                List.of(new CharacteristicValue(inverter.getCharacteristic().getId(), CharacteristicAmount.of("6")))
        );

        var edited = models.save(loaded);

        assertThat(edited.getVersion()).contains(1L);
        assertThat(models.findById(saved.getId()).orElseThrow().getValues())
                .extracting(value -> value.amount().getValue())
                .containsExactly(new BigDecimal("6.000"));
    }

    @Test
    @DisplayName("editar o tipo mantém o id da definição, que os valores dos modelos referenciam")
    void updatesTheTypeKeepingTheDefinitionId() {
        var huawei = savedManufacturer("Huawei");
        var inverter = savedType("Inversor", MeasurementUnit.KILOWATT);
        var definitionId = inverter.getCharacteristic().getId();

        savedModel(huawei, inverter, "SUN2000-5KTL", "5");

        var loaded = types.findById(inverter.getId()).orElseThrow();

        loaded.rename(TypeName.of("Inversor solar"));
        loaded.changeCharacteristic(
                CharacteristicName.of("Potência nominal"),
                MeasurementUnit.KILOWATT,
                false,
                true
        );

        types.save(loaded);

        var again = types.findById(inverter.getId()).orElseThrow();

        assertThat(again.getName()).isEqualTo(TypeName.of("Inversor solar"));
        assertThat(again.getCharacteristic().getId()).isEqualTo(definitionId);
        assertThat(again.getCharacteristic().getName()).isEqualTo(CharacteristicName.of("Potência nominal"));

        again.changeCharacteristic(
                CharacteristicName.of("Potência de saída"),
                MeasurementUnit.KILOWATT,
                false,
                true
        );

        types.save(again);

        assertThat(types.findById(inverter.getId()).orElseThrow().getCharacteristic().getName())
                .isEqualTo(CharacteristicName.of("Potência de saída"));
    }

    @Test
    @DisplayName("segundo fabricante com o mesmo nome em outra caixa é ConflictException com o código do nome")
    void secondManufacturerWithTheSameNameInAnotherCaseIsAConflict() {
        savedManufacturer("Huawei");

        assertThatThrownBy(() -> savedManufacturer("huawei"))
                .isInstanceOf(ConflictException.class)
                .hasMessage("já existe um fabricante com este nome")
                .satisfies(error -> assertThat(codeOf(error)).isEqualTo("manufacturer_name_taken"));
    }

    @Test
    @DisplayName("tipo e modelo com nome repetido em outra caixa saem com o código de cada um")
    void typeAndModelNameConflictsCarryTheirCode() {
        var huawei = savedManufacturer("Huawei");
        var inverter = savedType("Inversor", MeasurementUnit.KILOWATT);

        savedModel(huawei, inverter, "SUN2000-5KTL", null);

        assertThatThrownBy(() -> savedType("INVERSOR", MeasurementUnit.WATT))
                .isInstanceOf(ConflictException.class)
                .hasMessage("já existe um tipo com este nome")
                .satisfies(error -> assertThat(codeOf(error)).isEqualTo("type_name_taken"));
        assertThatThrownBy(() -> savedModel(huawei, inverter, "sun2000-5ktl", null))
                .isInstanceOf(ConflictException.class)
                .hasMessage("este fabricante já tem um modelo com esse nome")
                .satisfies(error -> assertThat(codeOf(error)).isEqualTo("model_name_taken"));
    }

    @Test
    @DisplayName("dois cadastros do mesmo nome ao mesmo tempo: um grava, o outro recebe ConflictException")
    void twoSavesOfTheSameNameAtTheSameTimeLeaveOneAndConflictTheOther() throws Exception {
        var attempts = 2;
        var ready = new CountDownLatch(attempts);
        var go = new CountDownLatch(1);
        var pool = Executors.newFixedThreadPool(attempts);

        try {
            var futures = new ArrayList<Future<Manufacturer>>();

            for (var i = 0; i < attempts; i++) {
                var name = i == 0 ? "Sungrow" : "SUNGROW";

                futures.add(pool.submit(() -> {
                    ready.countDown();
                    go.await();

                    return savedManufacturer(name);
                }));
            }

            ready.await();
            go.countDown();

            var winners = 0;
            var conflicts = new ArrayList<Throwable>();

            for (var future : futures) {
                try {
                    future.get();
                    winners++;
                }
                catch (ExecutionException e) {
                    conflicts.add(e.getCause());
                }
            }

            assertThat(winners).isEqualTo(1);
            assertThat(conflicts).singleElement()
                    .isInstanceOf(ConflictException.class)
                    .satisfies(error -> assertThat(codeOf(error)).isEqualTo("manufacturer_name_taken"));
            assertThat(jdbc.queryForObject("SELECT count(*) FROM manufacturers", Long.class)).isEqualTo(1L);
        }
        finally {
            pool.shutdownNow();
        }
    }

    @Test
    @DisplayName("o mesmo nome de modelo vale para outro fabricante")
    void sameModelNameForAnotherManufacturerIsAllowed() {
        var huawei = savedManufacturer("Huawei");
        var solis = savedManufacturer("Solis");
        var inverter = savedType("Inversor", MeasurementUnit.KILOWATT);

        savedModel(huawei, inverter, "Modelo X", null);
        savedModel(solis, inverter, "Modelo X", null);

        assertThat(jdbc.queryForObject("SELECT count(*) FROM equipment_models", Long.class)).isEqualTo(2L);
    }

    @Test
    @DisplayName("existsByName ignora a caixa e o id excetuado: renomear para o próprio nome não conflita")
    void existsByNameIgnoresTheExceptedId() {
        var huawei = savedManufacturer("Huawei");
        var inverter = savedType("Inversor", MeasurementUnit.KILOWATT);
        var model = savedModel(huawei, inverter, "SUN2000-5KTL", null);
        var other = UUID.randomUUID();

        assertThat(manufacturers.existsByName(ManufacturerName.of("huawei"), null)).isTrue();
        assertThat(manufacturers.existsByName(ManufacturerName.of("HUAWEI"), huawei.getId())).isFalse();
        assertThat(manufacturers.existsByName(ManufacturerName.of("Huawei"), other)).isTrue();
        assertThat(manufacturers.existsByName(ManufacturerName.of("Solis"), null)).isFalse();

        assertThat(types.existsByName(TypeName.of("INVERSOR"), null)).isTrue();
        assertThat(types.existsByName(TypeName.of("inversor"), inverter.getId())).isFalse();
        assertThat(types.existsByName(TypeName.of("Inversor"), other)).isTrue();

        assertThat(models.existsByManufacturerAndName(huawei.getId(), ModelName.of("sun2000-5ktl"), null)).isTrue();
        assertThat(models.existsByManufacturerAndName(huawei.getId(), ModelName.of("SUN2000-5KTL"), model.getId())).isFalse();
        assertThat(models.existsByManufacturerAndName(huawei.getId(), ModelName.of("SUN2000-5KTL"), other)).isTrue();
        assertThat(models.existsByManufacturerAndName(other, ModelName.of("SUN2000-5KTL"), null)).isFalse();
    }

    @Test
    @DisplayName("search de modelos filtra por tipo, situação e texto do modelo ou do fabricante; % e _ são literais")
    void searchFiltersByTypeStatusAndText() {
        var huawei = savedManufacturer("Huawei");
        var solis = savedManufacturer("Solis 100%");
        var inverter = savedType("Inversor", MeasurementUnit.KILOWATT);
        var panel = savedType("Painel", MeasurementUnit.WATT_PEAK);

        savedModel(huawei, inverter, "SUN2000_5KTL", "5");
        savedModel(solis, inverter, "S6-GR1P3K", null);
        savedModel(huawei, panel, "Painel 550", "550");

        var retired = savedModel(solis, panel, "Painel antigo", null);

        retired.deactivate();
        models.save(retired);

        var all = Pageable.of(0, 10);

        assertThat(names(modelReader.search(new EquipmentModelFilter(null, null, null), all).content()))
                .containsExactly("Painel 550", "S6-GR1P3K", "SUN2000_5KTL");
        assertThat(names(modelReader.search(new EquipmentModelFilter(null, inverter.getId(), StatusFilter.ACTIVE), all).content()))
                .containsExactly("S6-GR1P3K", "SUN2000_5KTL");
        assertThat(names(modelReader.search(new EquipmentModelFilter(null, null, StatusFilter.DEACTIVATED), all).content()))
                .containsExactly("Painel antigo");
        assertThat(modelReader.search(new EquipmentModelFilter(null, null, StatusFilter.ALL), all).totalElements())
                .isEqualTo(4);
        assertThat(names(modelReader.search(new EquipmentModelFilter("HUAWEI", null, null), all).content()))
                .containsExactly("Painel 550", "SUN2000_5KTL");
        assertThat(names(modelReader.search(new EquipmentModelFilter("gr1p", null, null), all).content()))
                .containsExactly("S6-GR1P3K");
        assertThat(names(modelReader.search(new EquipmentModelFilter("%", null, null), all).content()))
                .containsExactly("S6-GR1P3K");
        assertThat(names(modelReader.search(new EquipmentModelFilter("_", null, null), all).content()))
                .containsExactly("SUN2000_5KTL");
        assertThat(modelReader.search(new EquipmentModelFilter("sun2000-5", null, null), all).content()).isEmpty();

        var summary = modelReader.search(new EquipmentModelFilter("SUN2000", null, null), all).content().getFirst();

        assertThat(summary.manufacturer()).isEqualTo(new EquipmentModelSummary.Ref(huawei.getId(), "Huawei"));
        assertThat(summary.type()).isEqualTo(new EquipmentModelSummary.Ref(inverter.getId(), "Inversor"));
        assertThat(summary.values()).containsExactly(new EquipmentModelSummary.ValueView(
                inverter.getCharacteristic().getId(),
                "Potência",
                MeasurementUnit.KILOWATT,
                new BigDecimal("5.000")
        ));
        assertThat(modelReader.findSummary(summary.id())).contains(summary);
        assertThat(modelReader.findSummary(UUID.randomUUID())).isEmpty();
    }

    @Test
    @DisplayName("search pagina e ordena pelo nome; campo fora da lista vale o nome")
    void searchPaginatesAndSortsByName() {
        var huawei = savedManufacturer("Huawei");
        var inverter = savedType("Inversor", MeasurementUnit.KILOWATT);

        for (var i = 1; i <= 3; i++)
            savedModel(huawei, inverter, "Modelo " + i, null);

        var first = modelReader.search(new EquipmentModelFilter(null, null, null), Pageable.of(0, 2, "name", "DESC"));
        var injected = modelReader.search(new EquipmentModelFilter(null, null, null), Pageable.of(0, 3, "id; drop table x", "ASC"));

        assertThat(names(first.content())).containsExactly("Modelo 3", "Modelo 2");
        assertThat(first.totalElements()).isEqualTo(3);
        assertThat(first.totalPages()).isEqualTo(2);
        assertThat(names(injected.content())).containsExactly("Modelo 1", "Modelo 2", "Modelo 3");
    }

    @Test
    @DisplayName("hasModelsOfType diz se algum modelo, ativo ou não, usa o tipo")
    void hasModelsOfType() {
        var huawei = savedManufacturer("Huawei");
        var inverter = savedType("Inversor", MeasurementUnit.KILOWATT);
        var panel = savedType("Painel", MeasurementUnit.WATT_PEAK);
        var model = savedModel(huawei, inverter, "SUN2000-5KTL", null);

        model.deactivate();
        models.save(model);

        assertThat(modelReader.hasModelsOfType(inverter.getId())).isTrue();
        assertThat(modelReader.hasModelsOfType(panel.getId())).isFalse();
    }

    @Test
    @DisplayName("listas de fabricantes e de tipos contam os modelos e filtram pela situação")
    void manufacturerAndTypeListsCountModelsAndFilterByStatus() {
        var huawei = savedManufacturer("Huawei");
        var solis = manufacturers.findById(savedManufacturer("Solis").getId()).orElseThrow();
        var inverter = savedType("Inversor", MeasurementUnit.KILOWATT);
        var panel = types.findById(savedType("Painel", MeasurementUnit.WATT_PEAK).getId()).orElseThrow();

        savedModel(huawei, inverter, "SUN2000-5KTL", null);
        savedModel(huawei, inverter, "SUN2000-6KTL", null);

        solis.deactivate();
        manufacturers.save(solis);
        panel.deactivate();
        types.save(panel);

        var all = Pageable.of(0, 10);

        assertThat(manufacturerReader.search(new ManufacturerFilter(null, null), all).content())
                .containsExactly(new ManufacturerSummary(huawei.getId(), "Huawei", CatalogStatus.ACTIVE, 2));
        assertThat(manufacturerReader.search(new ManufacturerFilter("sol", StatusFilter.ALL), all).content())
                .containsExactly(new ManufacturerSummary(solis.getId(), "Solis", CatalogStatus.DEACTIVATED, 0));
        assertThat(manufacturerReader.search(new ManufacturerFilter(null, StatusFilter.ALL), all).totalElements())
                .isEqualTo(2);

        assertThat(typeReader.search(StatusFilter.ACTIVE, all).content()).containsExactly(new EquipmentTypeSummary(
                inverter.getId(),
                "Inversor",
                CatalogStatus.ACTIVE,
                2,
                List.of(new EquipmentTypeSummary.CharacteristicView(
                        inverter.getCharacteristic().getId(),
                        "Potência",
                        MeasurementUnit.KILOWATT,
                        false
                ))
        ));
        assertThat(typeReader.search(StatusFilter.DEACTIVATED, all).content())
                .extracting(EquipmentTypeSummary::name)
                .containsExactly("Painel");
        assertThat(typeReader.search(StatusFilter.ALL, all).totalElements()).isEqualTo(2);
    }

    private static List<String> names(List<EquipmentModelSummary> summaries) {
        return summaries.stream()
                .map(EquipmentModelSummary::name)
                .toList();
    }
}
