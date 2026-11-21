package com.crm.web.registry.requests;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

/** Corpo de {@code POST /catalog/equipment-models}. */
public record RegisterEquipmentModelRequest(
        @NotNull(message = "é obrigatório") UUID manufacturerId,
        @NotNull(message = "é obrigatório") UUID typeId,
        @NotBlank(message = "é obrigatório") @Size(min = 1, max = 80, message = "deve ter de 1 a 80 caracteres") String name,
        @NotNull(message = "é obrigatório") List<@Valid ValueRequest> values
) {
}
