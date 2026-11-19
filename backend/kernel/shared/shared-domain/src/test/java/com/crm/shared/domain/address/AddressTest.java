package com.crm.shared.domain.address;

import com.crm.common.domain.exceptions.ValidationException;
import com.crm.shared.domain.enums.UF;
import com.crm.shared.domain.vos.address.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Address")
class AddressTest {

    private static Address valid(AddressComplement complement) {
        return new Address(
                ZipCode.of("01310-100"),
                Street.of("Avenida Paulista"),
                AddressNumber.of("1578"),
                complement,
                Neighborhood.of("Bela Vista"),
                CityName.of("São Paulo"),
                UF.SP
        );
    }

    @Test
    @DisplayName("aceita sem complemento, que é o único sub-campo opcional")
    void withoutComplement() {
        var address = valid(null);

        assertNull(address.complement());
        assertEquals(CityName.of("São Paulo"), address.city());
    }

    @Test
    @DisplayName("aceita com complemento")
    void withComplement() {
        var address = valid(AddressComplement.of("Apto 101"));

        assertEquals(AddressComplement.of("Apto 101"), address.complement());
    }

    @ParameterizedTest
    @NullSource
    @DisplayName("recusa sem cep")
    void requiresZipCode(ZipCode missing) {
        assertThrows(ValidationException.class, () -> new Address(
                missing,
                Street.of("Avenida Paulista"),
                AddressNumber.of("1578"),
                null,
                Neighborhood.of("Bela Vista"),
                CityName.of("São Paulo"),
                UF.SP
        ));
    }

    @Test
    @DisplayName("recusa sem estado")
    void requiresState() {
        assertThrows(ValidationException.class, () -> new Address(
                ZipCode.of("01310-100"),
                Street.of("Avenida Paulista"),
                AddressNumber.of("1578"),
                null,
                Neighborhood.of("Bela Vista"),
                CityName.of("São Paulo"),
                null
        ));
    }
}

