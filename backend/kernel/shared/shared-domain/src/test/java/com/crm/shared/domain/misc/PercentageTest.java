package com.crm.shared.domain.misc;

import com.crm.common.domain.exceptions.ValidationException;
import com.crm.shared.domain.vos.misc.Money;
import com.crm.shared.domain.vos.misc.Percentage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("Percentage")
class PercentageTest {

    private static Percentage percentage(String value) {
        return Percentage.of(new BigDecimal(value));
    }

    @Test
    @DisplayName("guarda o número como se lê, não a fração")
    void storesReadableForm() {
        assertEquals(new BigDecimal("7.50"), percentage("7.5").getValue());
    }

    @Test
    @DisplayName("asFraction converte exatamente, sem arredondar")
    void convertsToFraction() {
        assertEquals(new BigDecimal("0.0750"), percentage("7.5").asFraction());
        assertEquals(new BigDecimal("1.0000"), percentage("100").asFraction());
        assertEquals(new BigDecimal("0.0000"), Percentage.zero().asFraction());
    }

    /** Acima de 100 quase sempre é fração confundida com percentual — erro caro se passar. */
    @Test
    @DisplayName("recusa acima de 100")
    void rejectsAboveHundred() {
        var exception = assertThrows(ValidationException.class, () -> percentage("100.01"));

        assertEquals("percentual não pode ser maior que 100", exception.getMessage());
    }

    @Test
    @DisplayName("recusa negativo e nulo")
    void rejectsNegativeAndNull() {
        assertEquals(
                "percentual não pode ser negativo",
                assertThrows(ValidationException.class, () -> percentage("-1")).getMessage()
        );

        assertThrows(ValidationException.class, () -> Percentage.of(null));
    }

    @Test
    @DisplayName("100% devolve o próprio valor")
    void hundredPercentIsIdentity() {
        var price = Money.of(new BigDecimal("42.37"));

        assertEquals(price, price.applyRate(percentage("100")));
    }
}
