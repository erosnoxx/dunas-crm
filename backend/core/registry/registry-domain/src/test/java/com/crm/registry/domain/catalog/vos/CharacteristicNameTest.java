package com.crm.registry.domain.catalog.vos;

import com.crm.common.domain.exceptions.ValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("CharacteristicName")
class CharacteristicNameTest {

    private static final String RANGE = "nome da característica deve ter entre 2 e 40 caracteres";

    @Test
    @DisplayName("aceita o limite inferior de 2 caracteres")
    void acceptsLowerLimit() {
        assertThat(CharacteristicName.of("a".repeat(2)).getValue()).hasSize(2);
    }

    @Test
    @DisplayName("aceita o limite superior de 40 caracteres")
    void acceptsUpperLimit() {
        assertThat(CharacteristicName.of("a".repeat(40)).getValue()).hasSize(40);
    }

    @Test
    @DisplayName("apara os espaços das pontas")
    void trimsEnds() {
        assertThat(CharacteristicName.of("  Abc  ").getValue()).isEqualTo("Abc");
    }

    @Test
    @DisplayName("recusa um caractere a menos que o limite inferior")
    void rejectsBelowLowerLimit() {
        assertThatThrownBy(() -> CharacteristicName.of("a".repeat(2 - 1)))
                .isInstanceOf(ValidationException.class)
                .hasMessage(RANGE);
    }
    @Test
    @DisplayName("recusa um caractere a mais que o limite superior")
    void rejectsAboveUpperLimit() {
        assertThatThrownBy(() -> CharacteristicName.of("a".repeat(40 + 1)))
                .isInstanceOf(ValidationException.class)
                .hasMessage(RANGE);
    }

    @Test
    @DisplayName("recusa nulo")
    void rejectsNull() {
        assertThatThrownBy(() -> CharacteristicName.of(null)).isInstanceOf(ValidationException.class);
    }

    @Test
    @DisplayName("recusa vazio e só espaços")
    void rejectsBlank() {
        assertThatThrownBy(() -> CharacteristicName.of(""))
                .isInstanceOf(ValidationException.class)
                .hasMessage("nome da característica não pode ser vazio");
        assertThatThrownBy(() -> CharacteristicName.of("   "))
                .isInstanceOf(ValidationException.class)
                .hasMessage("nome da característica não pode ser vazio");
    }
}
