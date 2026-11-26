package com.crm.web.registry.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Corpo de {@code POST /customers}. */
public record RegisterCustomerRequest(
        @NotBlank(message = "é obrigatório") @Size(max = 18, message = "deve ter no máximo 18 caracteres") String document,
        @NotBlank(message = "é obrigatório") @Size(min = 2, max = 150, message = "deve ter de 2 a 150 caracteres") String name,
        @Size(min = 2, max = 150, message = "deve ter de 2 a 150 caracteres") String tradeName,
        @Size(max = 254, message = "deve ter no máximo 254 caracteres") String email,
        @Size(max = 16, message = "deve ter no máximo 16 caracteres") String phone
) {
}
