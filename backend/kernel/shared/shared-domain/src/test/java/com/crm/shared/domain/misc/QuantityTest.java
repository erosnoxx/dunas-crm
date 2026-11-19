package com.crm.shared.domain.misc;

import com.crm.common.domain.exceptions.ValidationException;
import com.crm.shared.domain.vos.misc.Quantity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Quantity")
class QuantityTest {

    @Test
    @DisplayName("aceita inteiro e normaliza para escala zero")
    void acceptsIntegers() {
        assertEquals(new BigDecimal("3"), Quantity.of(3).getValue());
        assertEquals(new BigDecimal("3"), Quantity.of(new BigDecimal("3.000")).getValue());
        assertTrue(Quantity.zero().isZero());
    }

    /** Não existe meio ingresso: receber fração é defeito de quem chamou, não algo a truncar. */
    @Test
    @DisplayName("recusa valor fracionário em vez de truncar")
    void rejectsFractional() {
        var exception = assertThrows(
                ValidationException.class,
                () -> Quantity.of(new BigDecimal("2.5"))
        );

        assertEquals("quantidade deve possuir no máximo 0 casas decimais", exception.getMessage());
    }

    @Test
    @DisplayName("recusa nulo e negativo")
    void rejectsNullAndNegative() {
        assertEquals(
                "o valor não pode ser nulo",
                assertThrows(ValidationException.class, () -> Quantity.of(null)).getMessage()
        );

        assertEquals(
                "quantidade não pode ser negativo",
                assertThrows(ValidationException.class, () -> Quantity.of(-1)).getMessage()
        );
    }

    @Test
    @DisplayName("soma e subtrai devolvendo Quantity")
    void addsAndSubtracts() {
        assertEquals(Quantity.of(5), Quantity.of(2).add(Quantity.of(3)));
        assertEquals(Quantity.of(1), Quantity.of(3).subtract(Quantity.of(2)));
    }

    /** Estoque negativo é sempre defeito — melhor estourar na subtração do que persistir. */
    @Test
    @DisplayName("subtrair além do disponível é recusado")
    void cannotGoNegative() {
        assertEquals(
                "quantidade não pode ser negativo",
                assertThrows(
                        ValidationException.class,
                        () -> Quantity.of(1).subtract(Quantity.of(2))
                ).getMessage()
        );
    }

    @Test
    @DisplayName("asInt devolve o valor para uso em laço")
    void exposesInt() {
        assertEquals(3, Quantity.of(3).asInt());
    }
}
