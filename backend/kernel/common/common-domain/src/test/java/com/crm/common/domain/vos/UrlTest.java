package com.crm.common.domain.vos;

import com.crm.common.domain.exceptions.ValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("Url")
class UrlTest {

    @Test
    @DisplayName("aceita http e https absolutos")
    void acceptsHttpAndHttps() {
        assertEquals("http://exemplo.com", Url.of("http://exemplo.com").getValue());
        assertEquals("https://exemplo.com/caminho?query=1", Url.of("https://exemplo.com/caminho?query=1").getValue());
    }

    @Test
    @DisplayName("recusa esquema diferente de http/https")
    void rejectsOtherSchemes() {
        assertEquals(
                "url deve utilizar http ou https",
                assertThrows(ValidationException.class, () -> Url.of("ftp://exemplo.com")).getMessage()
        );

        assertThrows(ValidationException.class, () -> Url.of("javascript:alert(1)"));
    }

    @Test
    @DisplayName("recusa url relativa")
    void rejectsRelativeUrl() {
        assertEquals(
                "url deve ser absoluta",
                assertThrows(ValidationException.class, () -> Url.of("/caminho/relativo")).getMessage()
        );
    }

    @Test
    @DisplayName("recusa espaço em branco no meio")
    void rejectsWhitespace() {
        assertEquals(
                "url não pode conter espaços em branco",
                assertThrows(ValidationException.class, () -> Url.of("http://exemplo.com/a b")).getMessage()
        );
    }

    @Test
    @DisplayName("recusa sintaxe inválida e host ausente")
    void rejectsMalformed() {
        assertThrows(ValidationException.class, () -> Url.of("http://"));
        assertThrows(ValidationException.class, () -> Url.of("http:///caminho-sem-host"));
    }

    @Test
    @DisplayName("recusa nulo, vazio e acima do limite de tamanho")
    void rejectsNullBlankAndTooLong() {
        assertThrows(ValidationException.class, () -> Url.of(null));
        assertThrows(ValidationException.class, () -> Url.of(""));

        var tooLong = "https://exemplo.com/" + "a".repeat(2048);
        assertThrows(ValidationException.class, () -> Url.of(tooLong));
    }

    @Test
    @DisplayName("mensagem em pt-BR minúsculo")
    void messageFollowsStyle() {
        var message = assertThrows(ValidationException.class, () -> Url.of(null)).getMessage();

        assertEquals("o valor não pode ser nulo", message);
    }
}
