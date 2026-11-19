package com.crm.shared.domain.address;

import com.crm.common.domain.exceptions.ValidationException;
import com.crm.shared.domain.vos.address.AddressNumber;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("AddressNumber")
class AddressNumberTest {

    @Test
    @DisplayName("normaliza para maiúsculas")
    void normalizesToUppercase() {
        assertEquals("123A", AddressNumber.of("123a").getValue());
    }

    @Test
    @DisplayName("aceita S/N")
    void acceptsNoNumberMarker() {
        assertEquals("S/N", AddressNumber.of("s/n").getValue());
    }

    @Test
    @DisplayName("recusa acima do limite")
    void rejectsTooLong() {
        assertThrows(ValidationException.class, () -> AddressNumber.of("1".repeat(16)));
    }

    /** Regressão: toUpperCase() era chamado antes do guard de nulo da base. */
    @Test
    @DisplayName("recusa nulo sem lançar NullPointerException")
    void rejectsNullWithoutNativeException() {
        assertThrows(ValidationException.class, () -> AddressNumber.of(null));
    }
}
