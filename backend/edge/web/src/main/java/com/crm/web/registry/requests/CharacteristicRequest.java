package com.crm.web.registry.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Característica do tipo de equipamento. A unidade é validada pelo use case, que conhece os valores. */
public record CharacteristicRequest(
        @NotBlank(message = "é obrigatório") @Size(min = 2, max = 40, message = "deve ter de 2 a 40 caracteres") String name,
        @NotBlank(message = "é obrigatório") String unit,
        boolean required
) {
}
