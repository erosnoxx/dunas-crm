package com.crm.web.registry.requests;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/** Corpo de {@code PUT /catalog/equipment-models/{id}}: fabricante e tipo não mudam, então não vêm. */
public record UpdateEquipmentModelRequest(
        @NotBlank(message = "é obrigatório") @Size(min = 1, max = 80, message = "deve ter de 1 a 80 caracteres") String name,
        @NotNull(message = "é obrigatório") List<@Valid ValueRequest> values
) {
}
