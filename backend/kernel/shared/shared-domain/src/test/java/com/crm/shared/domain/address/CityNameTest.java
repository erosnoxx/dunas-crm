package com.crm.shared.domain.address;

import com.crm.common.domain.exceptions.ValidationException;
import com.crm.shared.domain.vos.address.CityName;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("CityName")
class CityNameTest {

    @Test
    @DisplayName("aceita valor dentro do intervalo")
    void acceptsValid() {
        assertEquals("São Paulo", CityName.of("São Paulo").getValue());
    }

    @Test
    @DisplayName("recusa acima do limite")
    void rejectsTooLong() {
        assertThrows(ValidationException.class, () -> CityName.of("a".repeat(101)));
    }

    @Test
    @DisplayName("recusa nulo e vazio")
    void rejectsNullAndBlank() {
        assertThrows(ValidationException.class, () -> CityName.of(null));
        assertThrows(ValidationException.class, () -> CityName.of(""));
    }
}
