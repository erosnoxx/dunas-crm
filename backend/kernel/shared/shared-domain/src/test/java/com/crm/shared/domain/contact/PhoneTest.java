package com.crm.shared.domain.contact;

import com.crm.common.domain.exceptions.ValidationException;
import com.crm.shared.domain.vos.contact.Phone;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Phone")
class PhoneTest {

    @Test
    @DisplayName("aceita E.164")
    void acceptsE164() {
        assertEquals("+5511999999999", Phone.of("+5511999999999").getValue());
        assertEquals("+12025550123", Phone.of("  +12025550123  ").getValue());
    }

    /**
     * Guardar sempre normalizado é o que torna a comparação possível: "(11) 99999-9999" e
     * "+5511999999999" são o mesmo telefone, e só um dos dois formatos pode existir no domínio.
     */
    @Test
    @DisplayName("recusa formato local com separadores")
    void rejectsLocalFormat() {
        for (var invalid : new String[]{"(11) 99999-9999", "11999999999", "+55 11 99999-9999", "+0119999999"}) {
            var exception = assertThrows(
                    ValidationException.class,
                    () -> Phone.of(invalid),
                    "deveria recusar: " + invalid
            );

            assertTrue(exception.getMessage().startsWith("formato de telefone inválido"));
        }
    }

    @Test
    @DisplayName("recusa nulo e vazio")
    void rejectsNullAndBlank() {
        assertThrows(ValidationException.class, () -> Phone.of(null));
        assertThrows(ValidationException.class, () -> Phone.of("  "));
    }
}
