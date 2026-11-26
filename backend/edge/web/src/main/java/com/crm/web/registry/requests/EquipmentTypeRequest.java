package com.crm.web.registry.requests;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Corpo de {@code POST /catalog/equipment-types} e de {@code PUT /catalog/equipment-types/{id}}. */
public record EquipmentTypeRequest(
        @NotBlank(message = "é obrigatório") @Size(min = 2, max = 60, message = "deve ter de 2 a 60 caracteres") String name,
        @NotNull(message = "é obrigatório") @Valid CharacteristicRequest characteristic
) {
}
