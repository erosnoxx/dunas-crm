package com.crm.registry.infrastructure.customers.mappers;

import com.crm.registry.domain.customers.entities.Location;
import com.crm.registry.domain.customers.enums.CustomerStatus;
import com.crm.registry.domain.customers.vos.LocationName;
import com.crm.shared.domain.enums.UF;
import com.crm.shared.domain.vos.address.Address;
import com.crm.shared.domain.vos.address.AddressComplement;
import com.crm.shared.domain.vos.address.AddressNumber;
import com.crm.shared.domain.vos.address.CityName;
import com.crm.shared.domain.vos.address.Neighborhood;
import com.crm.shared.domain.vos.address.Street;
import com.crm.shared.domain.vos.address.ZipCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("LocationMapper")
class LocationMapperTest {

    private final LocationMapper mapper = new LocationMapper();

    private static Address address(AddressComplement complement) {
        return new Address(
                ZipCode.of("01310100"),
                Street.of("Avenida Paulista"),
                AddressNumber.of("1000"),
                complement,
                Neighborhood.of("Bela Vista"),
                CityName.of("São Paulo"),
                UF.SP
        );
    }

    private static Location location(Long version, AddressComplement complement) {
        return Location.reconstitute(
                UUID.randomUUID(),
                version,
                UUID.randomUUID(),
                LocationName.of("Matriz"),
                address(complement),
                CustomerStatus.DEACTIVATED
        );
    }

    @Test
    @DisplayName("ida e volta preserva todos os campos, o endereço e a versão")
    void roundTripKeepsEveryField() {
        var location = location(2L, AddressComplement.of("Sala 12"));

        var back = mapper.toDomain(mapper.toPersistence(location));

        assertThat(back.getId()).isEqualTo(location.getId());
        assertThat(back.getVersion()).contains(2L);
        assertThat(back.getCustomerId()).isEqualTo(location.getCustomerId());
        assertThat(back.getName()).isEqualTo(location.getName());
        assertThat(back.getAddress()).isEqualTo(location.getAddress());
        assertThat(back.getStatus()).isEqualTo(CustomerStatus.DEACTIVATED);
    }

    @Test
    @DisplayName("versão nula é linha nova")
    void nullVersionIsNewRow() {
        var entity = mapper.toPersistence(location(null, null));

        assertThat(entity.getVersion()).isNull();
        assertThat(mapper.toDomain(entity).getVersion()).isEmpty();
    }

    @Test
    @DisplayName("complemento ausente vira coluna nula e volta ausente")
    void absentComplementStaysAbsent() {
        var entity = mapper.toPersistence(location(0L, null));

        assertThat(entity.getComplement()).isNull();
        assertThat(mapper.toDomain(entity).getAddress().complement()).isNull();
    }

    @Test
    @DisplayName("createdAt e updatedAt da persistência não chegam ao domínio")
    void timestampsStayInPersistence() {
        var entity = mapper.toPersistence(location(0L, null));

        entity.setCreatedAt(OffsetDateTime.parse("2026-01-01T00:00:00Z"));

        assertThat(mapper.toPersistence(mapper.toDomain(entity)).getCreatedAt()).isNull();
    }
}
