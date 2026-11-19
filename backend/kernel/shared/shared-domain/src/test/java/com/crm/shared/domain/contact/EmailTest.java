package com.crm.shared.domain.contact;

import com.crm.common.domain.exceptions.ValidationException;
import com.crm.shared.domain.vos.contact.Email;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("Email")
class EmailTest {

    /**
     * A normalização não é cosmética: o e-mail é a chave de unicidade de conta. Sem ela,
     * "Joao@x.com" e "joao@x.com" viram duas contas.
     */
    @Test
    @DisplayName("normaliza para minúsculas e remove espaços nas pontas")
    void normalizes() {
        assertEquals("joao@exemplo.com", Email.of("  Joao@Exemplo.COM  ").getValue());
        assertEquals(Email.of("joao@exemplo.com"), Email.of("JOAO@EXEMPLO.COM"));
    }

    @Test
    @DisplayName("aceita formatos válidos comuns")
    void acceptsValidFormats() {
        assertEquals("a.b+c@sub.dominio.com.br", Email.of("a.b+c@sub.dominio.com.br").getValue());
        assertEquals("joao_silva@dominio-x.com", Email.of("joao_silva@dominio-x.com").getValue());
    }

    @Test
    @DisplayName("recusa formatos inválidos")
    void rejectsInvalidFormats() {
        for (var invalid : new String[]{"sem-arroba", "@dominio.com", "joao@", "joao@dominio", "joao @x.com"}) {
            var exception = assertThrows(
                    ValidationException.class,
                    () -> Email.of(invalid),
                    "deveria recusar: " + invalid
            );

            assertEquals("formato de e-mail inválido", exception.getMessage());
        }
    }

    @Test
    @DisplayName("recusa nulo e vazio")
    void rejectsNullAndBlank() {
        assertThrows(ValidationException.class, () -> Email.of(null));
        assertThrows(ValidationException.class, () -> Email.of("   "));
    }
}
