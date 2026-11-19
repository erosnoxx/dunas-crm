package com.crm.iam.domain.vos;

import com.crm.common.domain.exceptions.ValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("DeviceLabel")
class DeviceLabelTest {

    @Test
    @DisplayName("aceita de 1 a 80 caracteres")
    void acceptsLimits() {
        assertThat(DeviceLabel.of("a").getValue()).isEqualTo("a");
        assertThat(DeviceLabel.of("Galaxy A54, app DUNAS").getValue()).isEqualTo("Galaxy A54, app DUNAS");
        assertThat(DeviceLabel.of("a".repeat(80)).getValue()).hasSize(80);
    }

    @Test
    @DisplayName("recusa nulo e vazio")
    void rejectsNullAndBlank() {
        assertThatThrownBy(() -> DeviceLabel.of(null)).isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> DeviceLabel.of(""))
                .isInstanceOf(ValidationException.class)
                .hasMessage("rótulo do aparelho não pode ser vazio");
    }

    @Test
    @DisplayName("recusa mais de 80 caracteres")
    void rejectsTooLong() {
        assertThatThrownBy(() -> DeviceLabel.of("a".repeat(81)))
                .isInstanceOf(ValidationException.class)
                .hasMessage("rótulo do aparelho deve ter entre 1 e 80 caracteres");
    }
}
