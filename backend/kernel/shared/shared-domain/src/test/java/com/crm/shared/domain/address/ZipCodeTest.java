package com.crm.shared.domain.address;

import com.crm.common.domain.exceptions.DomainException;
import com.crm.common.domain.exceptions.ValidationException;
import com.crm.shared.domain.vos.address.ZipCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("ZipCode")
class ZipCodeTest {

    @Test
    @DisplayName("remove máscara e guarda só dígitos")
    void stripsFormatting() {
        assertEquals("01310100", ZipCode.of("01310-100").getValue());
        assertEquals("01310100", ZipCode.of("01310100").getValue());
    }

    @Test
    @DisplayName("formatted devolve com hífen")
    void formatsForDisplay() {
        assertEquals("01310-100", ZipCode.of("01310100").formatted());
    }

    @Test
    @DisplayName("recusa comprimento diferente de 8")
    void rejectsWrongLength() {
        assertThrows(ValidationException.class, () -> ZipCode.of("123"));
        assertThrows(ValidationException.class, () -> ZipCode.of("012345678"));
    }

    @Test
    @DisplayName("mensagem em pt-BR minúsculo, não em inglês")
    void messageIsPortuguese() {
        var message = assertThrows(ValidationException.class, () -> ZipCode.of("123")).getMessage();

        assertEquals("cep deve ter entre 8 e 8 caracteres", message);
    }

    /**
     * Regressão: o VO original chamava replaceAll direto no valor, sem checar nulo antes —
     * NullPointerException nativa vazando do domínio em vez de ValidationException.
     */
    @Test
    @DisplayName("recusa nulo sem lançar NullPointerException")
    void rejectsNullWithoutNativeException() {
        assertThrows(ValidationException.class, () -> ZipCode.of(null));
    }
}
