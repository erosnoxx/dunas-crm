package com.crm.web.registry.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

/** Corpo de {@code POST /locations/{locationId}/equipment}; {@code installedOn} é {@code yyyy-MM-dd} e opcional. */
public record InstallEquipmentRequest(
        @NotNull(message = "é obrigatório") UUID modelId,
        @NotBlank(message = "é obrigatório") @Size(max = 60, message = "deve ter no máximo 60 caracteres") String serialNumber,
        LocalDate installedOn
) {
}
