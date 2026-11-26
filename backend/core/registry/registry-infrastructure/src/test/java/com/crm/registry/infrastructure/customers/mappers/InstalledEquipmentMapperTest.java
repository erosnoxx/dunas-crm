package com.crm.registry.infrastructure.customers.mappers;

import com.crm.registry.domain.customers.entities.InstalledEquipment;
import com.crm.registry.domain.customers.enums.CustomerStatus;
import com.crm.registry.domain.customers.vos.InstallationDate;
import com.crm.registry.domain.customers.vos.SerialNumber;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("InstalledEquipmentMapper")
class InstalledEquipmentMapperTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-21T12:00:00Z"), ZoneOffset.UTC);

    private final InstalledEquipmentMapper mapper = new InstalledEquipmentMapper();

    private static InstalledEquipment equipment(Long version, InstallationDate date) {
        return InstalledEquipment.reconstitute(
                UUID.randomUUID(),
                version,
                UUID.randomUUID(),
                UUID.randomUUID(),
                SerialNumber.of("SN-001"),
                date,
                CustomerStatus.DEACTIVATED
        );
    }

    @Test
    @DisplayName("ida e volta preserva todos os campos e a versão")
    void roundTripKeepsEveryField() {
        var equipment = equipment(5L, InstallationDate.of(LocalDate.parse("2026-11-01"), CLOCK));

        var back = mapper.toDomain(mapper.toPersistence(equipment));

        assertThat(back.getId()).isEqualTo(equipment.getId());
        assertThat(back.getVersion()).contains(5L);
        assertThat(back.getLocationId()).isEqualTo(equipment.getLocationId());
        assertThat(back.getModelId()).isEqualTo(equipment.getModelId());
        assertThat(back.getSerialNumber()).isEqualTo(equipment.getSerialNumber());
        assertThat(back.getInstalledOn()).isEqualTo(equipment.getInstalledOn());
        assertThat(back.getStatus()).isEqualTo(CustomerStatus.DEACTIVATED);
    }

    @Test
    @DisplayName("versão nula é linha nova")
    void nullVersionIsNewRow() {
        var entity = mapper.toPersistence(equipment(null, null));

        assertThat(entity.getVersion()).isNull();
        assertThat(mapper.toDomain(entity).getVersion()).isEmpty();
    }

    @Test
    @DisplayName("data de instalação ausente vira coluna nula e volta ausente")
    void absentInstallationDateStaysAbsent() {
        var entity = mapper.toPersistence(equipment(0L, null));

        assertThat(entity.getInstalledOn()).isNull();
        assertThat(mapper.toDomain(entity).getInstalledOn()).isEmpty();
    }

    @Test
    @DisplayName("createdAt e updatedAt da persistência não chegam ao domínio")
    void timestampsStayInPersistence() {
        var entity = mapper.toPersistence(equipment(0L, null));

        entity.setUpdatedAt(OffsetDateTime.parse("2026-01-02T00:00:00Z"));

        assertThat(mapper.toPersistence(mapper.toDomain(entity)).getUpdatedAt()).isNull();
    }
}
