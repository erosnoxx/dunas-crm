package com.crm.shared.domain.address;

import com.crm.common.domain.exceptions.ValidationException;
import com.crm.shared.domain.vos.address.AddressComplement;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("AddressComplement")
class AddressComplementTest {

    @Test
    @DisplayName("aceita valor dentro do intervalo")
    void acceptsValid() {
        assertEquals("Bloco B, Apto 42", AddressComplement.of("Bloco B, Apto 42").getValue());
    }

    @Test
    @DisplayName("recusa acima do limite")
    void rejectsTooLong() {
        assertThrows(ValidationException.class, () -> AddressComplement.of("a".repeat(51)));
    }

    @Test
    @DisplayName("recusa nulo e vazio — ausência de complemento é ausência da instância, não valor vazio")
    void rejectsNullAndBlank() {
        assertThrows(ValidationException.class, () -> AddressComplement.of(null));
        assertThrows(ValidationException.class, () -> AddressComplement.of(""));
    }
}
