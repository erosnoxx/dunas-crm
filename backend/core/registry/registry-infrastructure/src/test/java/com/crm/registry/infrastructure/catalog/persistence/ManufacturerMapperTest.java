package com.crm.registry.infrastructure.catalog.persistence;

import com.crm.registry.domain.catalog.entities.Manufacturer;
import com.crm.registry.domain.catalog.enums.CatalogStatus;
import com.crm.registry.domain.catalog.vos.ManufacturerName;
import com.crm.registry.infrastructure.catalog.mappers.ManufacturerMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ManufacturerMapper")
class ManufacturerMapperTest {

    private final ManufacturerMapper mapper = new ManufacturerMapper();

    @Test
    @DisplayName("ida e volta preserva todos os campos e a versão")
    void roundTripKeepsEveryField() {
        var manufacturer = Manufacturer.reconstitute(
                UUID.randomUUID(),
                4L,
                ManufacturerName.of("Huawei"),
                CatalogStatus.DEACTIVATED
        );

        var back = mapper.toDomain(mapper.toPersistence(manufacturer));

        assertThat(back.getId()).isEqualTo(manufacturer.getId());
        assertThat(back.getVersion()).contains(4L);
        assertThat(back.getName()).isEqualTo(manufacturer.getName());
        assertThat(back.getStatus()).isEqualTo(CatalogStatus.DEACTIVATED);
    }

    @Test
    @DisplayName("versão nula é linha nova")
    void nullVersionIsNewRow() {
        var entity = mapper.toPersistence(Manufacturer.register(ManufacturerName.of("Solis")));

        assertThat(entity.getVersion()).isNull();
        assertThat(mapper.toDomain(entity).getVersion()).isEmpty();
    }

    @Test
    @DisplayName("createdAt e updatedAt da persistência não chegam ao domínio")
    void timestampsStayInPersistence() {
        var entity = mapper.toPersistence(Manufacturer.register(ManufacturerName.of("Sungrow")));

        entity.setCreatedAt(OffsetDateTime.parse("2026-01-01T00:00:00Z"));
        entity.setUpdatedAt(OffsetDateTime.parse("2026-01-02T00:00:00Z"));

        var again = mapper.toPersistence(mapper.toDomain(entity));

        assertThat(again.getCreatedAt()).isNull();
        assertThat(again.getUpdatedAt()).isNull();
    }
}
