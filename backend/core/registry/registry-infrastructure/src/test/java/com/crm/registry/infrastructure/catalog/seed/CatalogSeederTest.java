package com.crm.registry.infrastructure.catalog.seed;

import com.crm.common.application.exceptions.InternalServerErrorException;
import com.crm.registry.application.catalog.contracts.repositories.EquipmentModelRepository;
import com.crm.registry.application.catalog.contracts.repositories.EquipmentTypeRepository;
import com.crm.registry.application.catalog.contracts.repositories.ManufacturerRepository;
import com.crm.registry.domain.catalog.entities.EquipmentModel;
import com.crm.registry.domain.catalog.entities.EquipmentType;
import com.crm.registry.domain.catalog.entities.Manufacturer;
import com.crm.registry.domain.catalog.enums.MeasurementUnit;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.core.io.ClassPathResource;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("CatalogSeeder")
class CatalogSeederTest {

    private ManufacturerRepository manufacturers;
    private EquipmentTypeRepository types;
    private EquipmentModelRepository models;
    private CatalogSeeder seeder;

    @BeforeEach
    void setUp() {
        manufacturers = mock(ManufacturerRepository.class);
        types = mock(EquipmentTypeRepository.class);
        models = mock(EquipmentModelRepository.class);

        seeder = new CatalogSeeder(manufacturers, types, models);
    }

    private static CatalogSeed inverterCatalog(String modelManufacturer, String amount) {
        return new CatalogSeed(
                List.of(new CatalogSeed.ManufacturerSeed("Huawei")),
                List.of(new CatalogSeed.TypeSeed("Inversor", "Potência", "KILOWATT", true)),
                List.of(new CatalogSeed.ModelSeed(modelManufacturer, "Inversor", "SUN2000-3KTL-L1", amount))
        );
    }

    @Test
    @DisplayName("aponta o JSON do seed e o tipo do registro")
    void pointsToTheSeedResource() {
        assertThat(seeder.resourcePath()).isEqualTo("seed/catalog.json");
        assertThat(seeder.recordType()).isEqualTo(CatalogSeed.class);
    }

    @Test
    @DisplayName("com fabricante ou tipo no banco já está semeado")
    void isAlreadySeededWhenThereIsAManufacturerOrAType() {
        when(manufacturers.count()).thenReturn(1L);
        when(types.count()).thenReturn(0L);
        assertThat(seeder.isAlreadySeeded()).isTrue();

        when(manufacturers.count()).thenReturn(0L);
        when(types.count()).thenReturn(1L);
        assertThat(seeder.isAlreadySeeded()).isTrue();
    }

    @Test
    @DisplayName("com o catálogo vazio ainda não está semeado")
    void isNotSeededWhenTheCatalogIsEmpty() {
        when(manufacturers.count()).thenReturn(0L);
        when(types.count()).thenReturn(0L);

        assertThat(seeder.isAlreadySeeded()).isFalse();
    }

    @Test
    @DisplayName("grava fabricantes, tipos e modelos pelo domínio")
    void seedsManufacturersTypesAndModelsThroughTheDomain() {
        seeder.seed(List.of(inverterCatalog("Huawei", "3")));

        var manufacturer = ArgumentCaptor.forClass(Manufacturer.class);
        var type = ArgumentCaptor.forClass(EquipmentType.class);
        var model = ArgumentCaptor.forClass(EquipmentModel.class);
        verify(manufacturers).save(manufacturer.capture());
        verify(types).save(type.capture());
        verify(models).save(model.capture());

        assertThat(manufacturer.getValue().getName().getValue()).isEqualTo("Huawei");
        assertThat(manufacturer.getValue().isActive()).isTrue();

        var characteristic = type.getValue().getCharacteristic();
        assertThat(type.getValue().getName().getValue()).isEqualTo("Inversor");
        assertThat(characteristic.getName().getValue()).isEqualTo("Potência");
        assertThat(characteristic.getUnit()).isEqualTo(MeasurementUnit.KILOWATT);
        assertThat(characteristic.isRequired()).isTrue();

        var saved = model.getValue();
        assertThat(saved.getName().getValue()).isEqualTo("SUN2000-3KTL-L1");
        assertThat(saved.getManufacturerId()).isEqualTo(manufacturer.getValue().getId());
        assertThat(saved.getTypeId()).isEqualTo(type.getValue().getId());
        assertThat(saved.getValues()).hasSize(1);
        assertThat(saved.getValues().getFirst().definitionId()).isEqualTo(characteristic.getId());
        assertThat(saved.getValues().getFirst().amount().getValue()).isEqualByComparingTo(new BigDecimal("3"));
    }

    @Test
    @DisplayName("modelo com fabricante fora da carga derruba o seed nomeando o modelo e não grava o modelo")
    void seedWithAnUnknownManufacturerInAModelFailsNamingTheModel() {
        assertThatThrownBy(() -> seeder.seed(List.of(inverterCatalog("Solis", "3"))))
                .isInstanceOf(InternalServerErrorException.class)
                .hasMessageContaining("SUN2000-3KTL-L1")
                .hasMessageNotContaining("Solis");

        verify(models, never()).save(any());
    }

    @Test
    @DisplayName("valor inválido derruba o seed nomeando o modelo, sem repetir o valor")
    void seedWithAnInvalidAmountFails() {
        assertThatThrownBy(() -> seeder.seed(List.of(inverterCatalog("Huawei", "-7.5"))))
                .isInstanceOf(InternalServerErrorException.class)
                .hasMessageContaining("SUN2000-3KTL-L1")
                .hasMessageNotContaining("-7.5");

        verify(models, never()).save(any());
    }

    @Test
    @DisplayName("unidade desconhecida derruba o seed nomeando o tipo")
    void seedWithAnUnknownUnitFailsNamingTheType() {
        var catalog = new CatalogSeed(
                List.of(),
                List.of(new CatalogSeed.TypeSeed("Inversor", "Potência", "PARSEC", true)),
                List.of()
        );

        assertThatThrownBy(() -> seeder.seed(List.of(catalog)))
                .isInstanceOf(InternalServerErrorException.class)
                .hasMessageContaining("Inversor")
                .hasMessageNotContaining("PARSEC");

        verify(types, never()).save(any());
    }

    @Test
    @DisplayName("lista vazia não semeia nada")
    void emptyListSeedsNothing() {
        seeder.seed(List.of());

        verify(manufacturers, never()).save(any());
        verify(types, never()).save(any());
        verify(models, never()).save(any());
    }

    @Test
    @DisplayName("o JSON embarcado é uma lista vazia e lê como o framework lê")
    void bundledJsonIsAnEmptyList() throws Exception {
        var mapper = new ObjectMapper();
        var type = mapper.getTypeFactory().constructCollectionType(List.class, seeder.recordType());

        List<CatalogSeed> seeds = mapper.readValue(new ClassPathResource(seeder.resourcePath()).getInputStream(), type);

        assertThat(seeds).isEmpty();
    }

    @Test
    @DisplayName("um item do JSON vira CatalogSeed com fabricantes, tipos e modelos")
    void jsonItemParsesToCatalogSeed() throws Exception {
        var json = """
                [{"manufacturers":[{"name":"Huawei"}],
                  "types":[{"name":"Inversor","characteristicName":"Potência","unit":"KILOWATT","required":true}],
                  "models":[{"manufacturer":"Huawei","type":"Inversor","name":"SUN2000-3KTL-L1","amount":"3"}]}]
                """;
        var mapper = new ObjectMapper();

        List<CatalogSeed> seeds = mapper.readValue(
                json,
                mapper.getTypeFactory().constructCollectionType(List.class, CatalogSeed.class)
        );

        assertThat(seeds).containsExactly(inverterCatalog("Huawei", "3"));
    }
}
