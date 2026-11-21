package com.crm.web.registry.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Corpo de {@code POST /catalog/manufacturers} e de {@code PUT /catalog/manufacturers/{id}}. */
public record ManufacturerRequest(
        @NotBlank(message = "é obrigatório") @Size(min = 2, max = 80, message = "deve ter de 2 a 80 caracteres") String name
) {
}
