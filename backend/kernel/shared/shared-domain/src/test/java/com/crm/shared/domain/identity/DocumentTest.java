package com.crm.shared.domain.identity;

import com.crm.common.domain.exceptions.ValidationException;
import com.crm.shared.domain.vos.identity.Document;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Document")
class DocumentTest {

    // CPF e CNPJ com dígitos verificadores válidos
    private static final String CPF = "52998224725";
    private static final String CNPJ = "11222333000181";
    // exemplo oficial da Receita para o CNPJ alfanumérico
    private static final String ALPHANUMERIC_CNPJ = "12ABC34501DE35";

    @Test
    @DisplayName("aceita CPF válido e identifica o tipo pelo comprimento")
    void acceptsCpf() {
        var document = Document.of(CPF);

        assertEquals(Document.DocumentType.CPF, document.getType());
        assertTrue(document.isIndividual());
        assertFalse(document.isOrganization());
    }

    @Test
    @DisplayName("aceita CNPJ válido e identifica o tipo pelo comprimento")
    void acceptsCnpj() {
        var document = Document.of(CNPJ);

        assertEquals(Document.DocumentType.CNPJ, document.getType());
        assertTrue(document.isOrganization());
        assertFalse(document.isIndividual());
    }

    @Test
    @DisplayName("aceita CNPJ alfanumérico válido, com ou sem máscara")
    void acceptsAlphanumericCnpj() {
        var document = Document.of(ALPHANUMERIC_CNPJ);

        assertEquals(Document.DocumentType.CNPJ, document.getType());
        assertEquals(document, Document.of("12.ABC.345/01DE-35"));
    }

    @Test
    @DisplayName("recusa CNPJ alfanumérico com dígito verificador inválido")
    void rejectsAlphanumericCnpjWithInvalidCheckDigits() {
        assertEquals(
                "cnpj inválido",
                assertThrows(ValidationException.class, () -> Document.of("12ABC34501DE36")).getMessage()
        );
    }

    /** Guardar só o significativo é o que permite comparar documento vindo de fontes diferentes. */
    @Test
    @DisplayName("remove máscara e compara por valor")
    void stripsFormatting() {
        assertEquals(CPF, Document.of("529.982.247-25").getValue());
        assertEquals(CNPJ, Document.of("11.222.333/0001-81").getValue());
        assertEquals(Document.of(CPF), Document.of("529.982.247-25"));
    }

    @Test
    @DisplayName("recusa dígito verificador inválido")
    void rejectsInvalidCheckDigits() {
        assertEquals(
                "cpf inválido",
                assertThrows(ValidationException.class, () -> Document.of("52998224726")).getMessage()
        );

        assertEquals(
                "cnpj inválido",
                assertThrows(ValidationException.class, () -> Document.of("11222333000182")).getMessage()
        );
    }

    /** Sequência repetida passa no cálculo do dígito, então precisa de recusa explícita. */
    @Test
    @DisplayName("recusa sequência de dígitos repetidos")
    void rejectsRepeatedDigits() {
        assertThrows(ValidationException.class, () -> Document.of("11111111111"));
        assertThrows(ValidationException.class, () -> Document.of("11111111111111"));
    }

    @Test
    @DisplayName("recusa comprimento que não é de CPF nem de CNPJ")
    void rejectsUnknownLength() {
        var exception = assertThrows(ValidationException.class, () -> Document.of("123"));

        assertEquals("documento inválido: deve ser um cpf ou cnpj válido", exception.getMessage());
    }

    @Test
    @DisplayName("recusa nulo")
    void rejectsNull() {
        assertEquals(
                "documento não pode ser nulo",
                assertThrows(ValidationException.class, () -> Document.of(null)).getMessage()
        );
    }

    @Test
    @DisplayName("maskedForDisplay mostra só os últimos 3 dígitos, o resto vira asterisco")
    void masksForDisplay() {
        assertEquals("********725", Document.of(CPF).maskedForDisplay());
        assertEquals("***********181", Document.of(CNPJ).maskedForDisplay());
    }
}
