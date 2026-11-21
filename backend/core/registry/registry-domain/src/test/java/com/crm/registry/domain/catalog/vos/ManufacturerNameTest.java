package com.crm.registry.domain.catalog.vos;

import com.crm.common.domain.exceptions.ValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ManufacturerName")
class ManufacturerNameTest {

    private static final String RANGE = "nome do fabricante deve ter entre 2 e 80 caracteres";

    @Test
    @DisplayName("aceita o limite inferior de 2 caracteres")
    void acceptsLowerLimit() {
        assertThat(ManufacturerName.of("a".repeat(2)).getValue()).hasSize(2);
    }

    @Test
    @DisplayName("aceita o limite superior de 80 caracteres")
    void acceptsUpperLimit() {
        assertThat(ManufacturerName.of("a".repeat(80)).getValue()).hasSize(80);
    }

    @Test
    @DisplayName("apara os espaços das pontas")
    void trimsEnds() {
        assertThat(ManufacturerName.of("  Abc  ").getValue()).isEqualTo("Abc");
    }

    @Test
    @DisplayName("recusa um caractere a menos que o limite inferior")
    void rejectsBelowLowerLimit() {
        assertThatThrownBy(() -> ManufacturerName.of("a".repeat(2 - 1)))
                .isInstanceOf(ValidationException.class)
                .hasMessage(RANGE);
    }
    @Test
    @DisplayName("recusa um caractere a mais que o limite superior")
    void rejectsAboveUpperLimit() {
        assertThatThrownBy(() -> ManufacturerName.of("a".repeat(80 + 1)))
                .isInstanceOf(ValidationException.class)
                .hasMessage(RANGE);
    }

    @Test
    @DisplayName("recusa nulo")
    void rejectsNull() {
        assertThatThrownBy(() -> ManufacturerName.of(null)).isInstanceOf(ValidationException.class);
    }

    @Test
    @DisplayName("recusa vazio e só espaços")
    void rejectsBlank() {
        assertThatThrownBy(() -> ManufacturerName.of(""))
                .isInstanceOf(ValidationException.class)
                .hasMessage("nome do fabricante não pode ser vazio");
        assertThatThrownBy(() -> ManufacturerName.of("   "))
                .isInstanceOf(ValidationException.class)
                .hasMessage("nome do fabricante não pode ser vazio");
    }
}
