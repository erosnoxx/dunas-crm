package com.crm.registry.domain.catalog.vos;

import com.crm.common.domain.exceptions.ValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("CharacteristicAmount")
class CharacteristicAmountTest {

    private static final String MESSAGE = "o valor precisa ser maior que zero, com até 3 casas decimais";

    private static void assertInvalid(Runnable action) {
        assertThatThrownBy(action::run)
                .isInstanceOfSatisfying(ValidationException.class, e -> {
                    assertThat(e.getCode()).contains("invalid_amount");
                    assertThat(e.getMessage()).isEqualTo(MESSAGE);
                });
    }

    @ParameterizedTest(name = "aceita {0}")
    @DisplayName("aceita valores positivos dentro dos limites")
    @ValueSource(strings = {"3", "12000", "0.001", "1.200", "999999999.999", "1.2000"})
    void acceptsValidText(String text) {
        assertThat(CharacteristicAmount.of(text).getValue()).isEqualByComparingTo(new BigDecimal(text));
    }

    @Test
    @DisplayName("normaliza para 3 casas decimais")
    void normalizesScale() {
        assertThat(CharacteristicAmount.of("3").getValue()).hasToString("3.000");
        assertThat(CharacteristicAmount.of(new BigDecimal("12000")).getValue()).hasToString("12000.000");
    }

    @Test
    @DisplayName("é igual por valor, ignorando zeros à direita")
    void equalsByValue() {
        assertThat(CharacteristicAmount.of("3")).isEqualTo(CharacteristicAmount.of(new BigDecimal("3.000")));
    }

    @ParameterizedTest(name = "recusa {0}")
    @DisplayName("recusa zero, negativo, mais de 3 casas e mais de 9 dígitos inteiros")
    @ValueSource(strings = {"0", "0.000", "-1", "-0.001", "1.2345", "0.0001", "1000000000", "1000000000.5"})
    void rejectsInvalidText(String text) {
        assertInvalid(() -> CharacteristicAmount.of(text));
        assertInvalid(() -> CharacteristicAmount.of(new BigDecimal(text)));
    }

    @ParameterizedTest(name = "recusa o texto \"{0}\"")
    @DisplayName("recusa texto não numérico e vazio")
    @ValueSource(strings = {"abc", "", "   ", "1,5", "1.2.3"})
    void rejectsNonNumericText(String text) {
        assertInvalid(() -> CharacteristicAmount.of(text));
    }

    @Test
    @DisplayName("recusa nulo, em texto e em decimal")
    void rejectsNull() {
        assertInvalid(() -> CharacteristicAmount.of((String) null));
        assertInvalid(() -> CharacteristicAmount.of((BigDecimal) null));
    }
}
