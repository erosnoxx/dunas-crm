package com.crm.shared.domain.address;

import com.crm.common.domain.exceptions.ValidationException;
import com.crm.shared.domain.vos.address.Street;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("Street")
class StreetTest {

    @Test
    @DisplayName("aceita logradouro dentro do intervalo")
    void acceptsValid() {
        assertEquals("Avenida Paulista", Street.of("Avenida Paulista").getValue());
    }

    @Test
    @DisplayName("recusa muito curto ou muito longo")
    void rejectsOutOfRange() {
        assertThrows(ValidationException.class, () -> Street.of("A"));
        assertThrows(ValidationException.class, () -> Street.of("A".repeat(151)));
    }

    @Test
    @DisplayName("recusa nulo e vazio")
    void rejectsNullAndBlank() {
        assertThrows(ValidationException.class, () -> Street.of(null));
        assertThrows(ValidationException.class, () -> Street.of("   "));
    }
}
