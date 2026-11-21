package com.crm.registry.domain.catalog.vos;

import com.crm.common.domain.exceptions.ValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ModelName")
class ModelNameTest {

    private static final String RANGE = "nome do modelo deve ter entre 1 e 80 caracteres";

    @Test
    @DisplayName("aceita o limite inferior de 1 caractere")
    void acceptsLowerLimit() {
        assertThat(ModelName.of("a".repeat(1)).getValue()).hasSize(1);
    }

    @Test
    @DisplayName("aceita o limite superior de 80 caracteres")
    void acceptsUpperLimit() {
        assertThat(ModelName.of("a".repeat(80)).getValue()).hasSize(80);
    }

    @Test
    @DisplayName("apara os espaços das pontas")
    void trimsEnds() {
        assertThat(ModelName.of("  Abc  ").getValue()).isEqualTo("Abc");
    }

    @Test
    @DisplayName("recusa um caractere a mais que o limite superior")
    void rejectsAboveUpperLimit() {
        assertThatThrownBy(() -> ModelName.of("a".repeat(80 + 1)))
                .isInstanceOf(ValidationException.class)
                .hasMessage(RANGE);
    }

    @Test
    @DisplayName("recusa nulo")
    void rejectsNull() {
        assertThatThrownBy(() -> ModelName.of(null)).isInstanceOf(ValidationException.class);
    }

    @Test
    @DisplayName("recusa vazio e só espaços")
    void rejectsBlank() {
        assertThatThrownBy(() -> ModelName.of(""))
                .isInstanceOf(ValidationException.class)
                .hasMessage("nome do modelo não pode ser vazio");
        assertThatThrownBy(() -> ModelName.of("   "))
                .isInstanceOf(ValidationException.class)
                .hasMessage("nome do modelo não pode ser vazio");
    }
}
