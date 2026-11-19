package com.crm.shared.domain.misc;

import com.crm.common.domain.exceptions.ValidationException;
import com.crm.shared.domain.vos.misc.Money;
import com.crm.shared.domain.vos.misc.Percentage;
import com.crm.shared.domain.vos.misc.Quantity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Money")
class MoneyTest {

    private static Money money(String value) {
        return Money.of(new BigDecimal(value));
    }

    private static void assertValue(String expected, Money actual) {
        assertEquals(new BigDecimal(expected), actual.getValue());
    }

    @Nested
    @DisplayName("entrada")
    class Input {

        @Test
        @DisplayName("normaliza para duas casas decimais")
        void normalizesScale() {
            assertValue("10.00", money("10"));
            assertValue("10.50", money("10.5"));
            assertValue("10.00", money("10.00"));
        }

        /**
         * Recusar, e não arredondar: preço com três casas vindo de uma API ou do banco e defeito,
         * e arredondar em silencio esconderia o defeito.
         */
        @Test
        @DisplayName("recusa valor com mais de duas casas significativas")
        void rejectsExcessiveScale() {
            var exception = assertThrows(ValidationException.class, () -> money("10.999"));

            assertEquals("valor monetário deve possuir no máximo 2 casas decimais", exception.getMessage());
        }

        /** Zeros à direita não são casas significativas — "10.0000" é dez reais. */
        @Test
        @DisplayName("aceita zeros à direita além da escala")
        void acceptsTrailingZeros() {
            assertValue("10.00", money("10.0000"));
        }

        @Test
        @DisplayName("recusa nulo e negativo")
        void rejectsNullAndNegative() {
            assertEquals(
                    "o valor não pode ser nulo",
                    assertThrows(ValidationException.class, () -> Money.of(null)).getMessage()
            );

            assertEquals(
                    "valor monetário não pode ser negativo",
                    assertThrows(ValidationException.class, () -> money("-0.01")).getMessage()
            );
        }

        @Test
        @DisplayName("mensagem em pt-BR minúsculo, sem ponto final")
        void messageFollowsStyle() {
            var message = assertThrows(ValidationException.class, () -> Money.of(null)).getMessage();

            assertTrue(Character.isLowerCase(message.charAt(0)), "deve começar minúscula: " + message);
            assertFalse(message.endsWith("."), "não deve terminar com ponto: " + message);
        }

        @Test
        @DisplayName("zero() é um valor válido")
        void zeroIsValid() {
            assertValue("0.00", Money.zero());
            assertTrue(Money.zero().isZero());
        }
    }

    @Nested
    @DisplayName("aritmética")
    class Arithmetic {

        @Test
        @DisplayName("operações devolvem Money, não BigDecimal cru")
        void operationsReturnMoney() {
            Money result = money("10.00").add(money("0.55"));

            assertValue("10.55", result);
        }

        @Test
        @DisplayName("é imutável — a operação não altera o operando")
        void isImmutable() {
            var original = money("10.00");

            original.add(money("5.00"));

            assertValue("10.00", original);
        }

        @Test
        @DisplayName("total de linha: preço vezes quantidade, exato")
        void lineTotal() {
            assertValue("30.00", money("10.00").times(Quantity.of(3)));
            assertValue("0.00", money("10.00").times(Quantity.zero()));
        }

        /**
         * Regressão: com {@code RoundingMode.UNNECESSARY} na base, qualquer percentual que não
         * fechasse exato lançava {@code ArithmeticException} nativa. Taxa sobre preço é o caso
         * central do produto (split da plataforma), então isto não pode voltar a quebrar.
         */
        @Test
        @DisplayName("taxa sobre preço arredonda em vez de estourar")
        void rateRounds() {
            assertValue("0.75", money("10.00").applyRate(Percentage.of(new BigDecimal("7.5"))));
            assertValue("0.33", money("10.00").applyRate(Percentage.of(new BigDecimal("3.33"))));
        }

        @Test
        @DisplayName("HALF_UP: metade arredonda para cima")
        void roundsHalfUp() {
            // 0.25 * 50% = 0.125 -> 0.13
            assertValue("0.13", money("0.25").applyRate(Percentage.of(new BigDecimal("50"))));
        }

        @Test
        @DisplayName("subtração que ficaria negativa é recusada, não devolve negativo")
        void subtractionCannotGoNegative() {
            var exception = assertThrows(
                    ValidationException.class,
                    () -> money("1.00").subtract(money("2.00"))
            );

            assertEquals("valor monetário não pode ser negativo", exception.getMessage());
        }

        /** Nenhuma exception nativa pode vazar do domínio, nem no caso degenerado. */
        @Test
        @DisplayName("operando nulo vira erro de domínio, não NullPointerException")
        void guardsNullOperand() {
            assertThrows(ValidationException.class, () -> money("10.00").add(null));
            assertThrows(ValidationException.class, () -> money("10.00").subtract(null));
            assertThrows(ValidationException.class, () -> money("10.00").isGreaterThan(null));
        }
    }

    @Nested
    @DisplayName("comparação e igualdade")
    class Comparison {

        @Test
        @DisplayName("compara por valor")
        void comparesByValue() {
            assertTrue(money("10.00").isGreaterThan(money("9.99")));
            assertTrue(money("9.99").isLessThan(money("10.00")));
            assertTrue(money("10.00").isGreaterThanOrEqualTo(money("10.00")));
            assertTrue(money("10.00").isLessThanOrEqualTo(money("10.00")));
        }

        /** "10" e "10.00" são o mesmo dinheiro: a normalização na entrada garante isso. */
        @Test
        @DisplayName("igualdade não depende da escala informada")
        void equalityIgnoresInputScale() {
            assertEquals(money("10"), money("10.00"));
            assertEquals(money("10").hashCode(), money("10.00").hashCode());
        }
    }
}
