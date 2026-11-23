package com.crm.registry.domain.customers.vos;

import com.crm.common.domain.exceptions.ValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("SerialNumber")
class SerialNumberTest {

    private static final String RANGE = "número de série deve ter entre 1 e 60 caracteres";

    @Test
    @DisplayName("aceita o limite inferior de 1 caractere(s)")
    void acceptsLowerLimit() {
        assertThat(SerialNumber.of("a".repeat(1)).getValue()).hasSize(1);
    }

    @Test
    @DisplayName("aceita o limite superior de 60 caracteres")
    void acceptsUpperLimit() {
        assertThat(SerialNumber.of("a".repeat(60)).getValue()).hasSize(60);
    }

    @Test
    @DisplayName("apara os espaços das pontas")
    void trimsEnds() {
        assertThat(SerialNumber.of("  Abc  ").getValue()).isEqualTo("Abc");
    }

    @Test
    @DisplayName("recusa um caractere a mais que o limite superior")
    void rejectsAboveUpperLimit() {
        assertThatThrownBy(() -> SerialNumber.of("a".repeat(60 + 1)))
                .isInstanceOf(ValidationException.class)
                .hasMessage(RANGE);
    }

    @Test
    @DisplayName("recusa nulo")
    void rejectsNull() {
        assertThatThrownBy(() -> SerialNumber.of(null)).isInstanceOf(ValidationException.class);
    }

    @Test
    @DisplayName("recusa só espaços")
    void rejectsBlank() {
        assertThatThrownBy(() -> SerialNumber.of("   ")).isInstanceOf(ValidationException.class);
    }

    @Test
    @DisplayName("mantém a caixa como digitada")
    void keepsTheCaseAsTyped() {
        assertThat(SerialNumber.of("hv22Ab").getValue()).isEqualTo("hv22Ab");
    }
}
