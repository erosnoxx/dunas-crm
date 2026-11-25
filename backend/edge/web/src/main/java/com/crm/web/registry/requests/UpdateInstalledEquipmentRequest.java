package com.crm.web.registry.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/** Corpo de {@code PUT /installed-equipment/{id}}; {@code installedOn} é {@code yyyy-MM-dd} e opcional. */
public record UpdateInstalledEquipmentRequest(
        @NotBlank(message = "é obrigatório") @Size(max = 60, message = "deve ter no máximo 60 caracteres") String serialNumber,
        LocalDate installedOn
) {
}
