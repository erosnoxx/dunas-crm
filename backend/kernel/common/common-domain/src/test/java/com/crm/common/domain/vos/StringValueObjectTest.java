package com.crm.common.domain.vos;

import com.crm.common.domain.exceptions.ValidationException;
import com.crm.common.domain.exceptions.DomainException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("StringValueObject")
class StringValueObjectTest {

    private static final class Name extends StringValueObject {
        private Name(String value) {
            super(value);
        }

        static Name of(String value) {
            return new Name(value);
        }

        @Override
        protected String type() {
            return "nome";
        }

        @Override
        protected void customValidate(String value) {
            checkLength(value, 3, 10);
        }
    }

    /** VO sem regra própria, para separar o comportamento da base do da subclasse. */
    private static final class FreeText extends StringValueObject {
        private FreeText(String value) {
            super(value);
        }

        static FreeText of(String value) {
            return new FreeText(value);
        }

        @Override
        protected String type() {
            return "texto";
        }
    }

    @Test
    @DisplayName("normaliza o valor removendo espaços nas pontas")
    void trimsValue() {
        assertEquals("abcd", Name.of("  abcd  ").getValue());
    }

    @Test
    @DisplayName("rejeita nulo na base, sem depender do type()")
    void rejectsNull() {
        var exception = assertThrows(ValidationException.class, () -> FreeText.of(null));

        assertEquals("o valor não pode ser nulo", exception.getMessage());
    }

    @Test
    @DisplayName("rejeita string vazia e string só de espaços")
    void rejectsBlank() {
        assertEquals(
                "texto não pode ser vazio",
                assertThrows(DomainException.class, () -> FreeText.of("")).getMessage()
        );

        assertThrows(DomainException.class, () -> FreeText.of("     "));
    }

    @Test
    @DisplayName("chama customValidate da subclasse")
    void runsCustomValidate() {
        assertEquals(
                "nome deve ter entre 3 e 10 caracteres",
                assertThrows(DomainException.class, () -> Name.of("ab")).getMessage()
        );
        assertThrows(DomainException.class, () -> Name.of("abcdefghijk"));

        assertEquals("abc", Name.of("abc").getValue());
    }

    /**
     * Regressão: customValidate recebia o valor cru, então um valor cujo comprimento só passava
     * por causa dos espaços era aceito e guardado curto.
     */
    @Test
    @DisplayName("valida o valor já normalizado, não o valor cru")
    void validatesTrimmedValue() {
        assertThrows(DomainException.class, () -> Name.of("  ab  "));
    }

    /**
     * Regressão: a base antiga usava {@code getClass().getSimpleName()} nas três mensagens
     * (nulo, vazio, checkLength), produzindo o nome da classe Java em inglês e capitalizado
     * (ex. "EventName não pode ser nulo"). Um teste anterior aqui só checava o FIM da mensagem
     * (`endsWith`) e por isso nunca pegou o defeito — checar também o início é o que garante que
     * {@code type()} está sendo usado, e não o nome da classe.
     */
    @Test
    @DisplayName("mensagem usa type(), não o nome da classe Java")
    void messageUsesTypeNotClassName() {
        var blankMessage = assertThrows(DomainException.class, () -> FreeText.of("   ")).getMessage();
        var lengthMessage = assertThrows(DomainException.class, () -> Name.of("a")).getMessage();

        assertTrue(blankMessage.startsWith("texto"), "deveria começar com o type(): " + blankMessage);
        assertTrue(lengthMessage.startsWith("nome"), "deveria começar com o type(): " + lengthMessage);

        assertTrue(Character.isLowerCase(blankMessage.charAt(0)), "deve começar minúscula: " + blankMessage);
        assertTrue(Character.isLowerCase(lengthMessage.charAt(0)), "deve começar minúscula: " + lengthMessage);

        assertFalse(blankMessage.endsWith("."), "não deve terminar com ponto: " + blankMessage);
    }

    @Test
    @DisplayName("igualdade é por valor, e tipos diferentes com o mesmo valor não são iguais")
    void equalityIsByValue() {
        assertEquals(FreeText.of("abc"), FreeText.of("abc"));
        assertEquals(FreeText.of("abc").hashCode(), FreeText.of("abc").hashCode());

        assertNotEquals(FreeText.of("abc"), FreeText.of("abd"));
        assertNotEquals(FreeText.of("abc"), Name.of("abc"));
    }

    @Test
    @DisplayName("igualdade considera o valor normalizado")
    void equalityUsesNormalizedValue() {
        assertEquals(FreeText.of("abc"), FreeText.of("  abc  "));
    }
}
