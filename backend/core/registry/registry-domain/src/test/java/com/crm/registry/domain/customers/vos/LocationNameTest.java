package com.crm.registry.domain.customers.vos;

import com.crm.common.domain.exceptions.ValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("LocationName")
class LocationNameTest {

    private static final String RANGE = "nome do local deve ter entre 2 e 60 caracteres";

    @Test
    @DisplayName("aceita o limite inferior de 2 caractere(s)")
    void acceptsLowerLimit() {
        assertThat(LocationName.of("a".repeat(2)).getValue()).hasSize(2);
    }

    @Test
    @DisplayName("aceita o limite superior de 60 caracteres")
    void acceptsUpperLimit() {
        assertThat(LocationName.of("a".repeat(60)).getValue()).hasSize(60);
    }

    @Test
    @DisplayName("apara os espaços das pontas")
    void trimsEnds() {
        assertThat(LocationName.of("  Abc  ").getValue()).isEqualTo("Abc");
    }

    @Test
    @DisplayName("recusa um caractere a mais que o limite superior")
    void rejectsAboveUpperLimit() {
        assertThatThrownBy(() -> LocationName.of("a".repeat(60 + 1)))
                .isInstanceOf(ValidationException.class)
                .hasMessage(RANGE);
    }

    @Test
    @DisplayName("recusa nulo")
    void rejectsNull() {
        assertThatThrownBy(() -> LocationName.of(null)).isInstanceOf(ValidationException.class);
    }

    @Test
    @DisplayName("recusa só espaços")
    void rejectsBlank() {
        assertThatThrownBy(() -> LocationName.of("   ")).isInstanceOf(ValidationException.class);
    }

    @Test
    @DisplayName("recusa um caractere a menos que o limite inferior")
    void rejectsBelowLowerLimit() {
        assertThatThrownBy(() -> LocationName.of("a"))
                .isInstanceOf(ValidationException.class)
                .hasMessage(RANGE);
    }
}
