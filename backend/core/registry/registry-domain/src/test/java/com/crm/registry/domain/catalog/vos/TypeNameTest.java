package com.crm.registry.domain.catalog.vos;

import com.crm.common.domain.exceptions.ValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("TypeName")
class TypeNameTest {

    private static final String RANGE = "nome do tipo deve ter entre 2 e 60 caracteres";

    @Test
    @DisplayName("aceita o limite inferior de 2 caracteres")
    void acceptsLowerLimit() {
        assertThat(TypeName.of("a".repeat(2)).getValue()).hasSize(2);
    }

    @Test
    @DisplayName("aceita o limite superior de 60 caracteres")
    void acceptsUpperLimit() {
        assertThat(TypeName.of("a".repeat(60)).getValue()).hasSize(60);
    }

    @Test
    @DisplayName("apara os espaços das pontas")
    void trimsEnds() {
        assertThat(TypeName.of("  Abc  ").getValue()).isEqualTo("Abc");
    }

    @Test
    @DisplayName("recusa um caractere a menos que o limite inferior")
    void rejectsBelowLowerLimit() {
        assertThatThrownBy(() -> TypeName.of("a".repeat(2 - 1)))
                .isInstanceOf(ValidationException.class)
                .hasMessage(RANGE);
    }
    @Test
    @DisplayName("recusa um caractere a mais que o limite superior")
    void rejectsAboveUpperLimit() {
        assertThatThrownBy(() -> TypeName.of("a".repeat(60 + 1)))
                .isInstanceOf(ValidationException.class)
                .hasMessage(RANGE);
    }

    @Test
    @DisplayName("recusa nulo")
    void rejectsNull() {
        assertThatThrownBy(() -> TypeName.of(null)).isInstanceOf(ValidationException.class);
    }

    @Test
    @DisplayName("recusa vazio e só espaços")
    void rejectsBlank() {
        assertThatThrownBy(() -> TypeName.of(""))
                .isInstanceOf(ValidationException.class)
                .hasMessage("nome do tipo não pode ser vazio");
        assertThatThrownBy(() -> TypeName.of("   "))
                .isInstanceOf(ValidationException.class)
                .hasMessage("nome do tipo não pode ser vazio");
    }
}
