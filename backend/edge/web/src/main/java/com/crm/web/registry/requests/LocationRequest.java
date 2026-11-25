package com.crm.web.registry.requests;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Corpo de {@code POST /customers/{customerId}/locations} e de {@code PUT /locations/{id}}. */
public record LocationRequest(
        @NotBlank(message = "é obrigatório") @Size(min = 2, max = 60, message = "deve ter de 2 a 60 caracteres") String name,
        @NotNull(message = "é obrigatório") @Valid AddressRequest address
) {
}
