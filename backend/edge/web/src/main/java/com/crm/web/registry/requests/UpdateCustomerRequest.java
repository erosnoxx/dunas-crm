package com.crm.web.registry.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Corpo de {@code PUT /customers/{id}}; o documento não muda, então um {@code document} no corpo é ignorado. */
public record UpdateCustomerRequest(
        @NotBlank(message = "é obrigatório") @Size(min = 2, max = 150, message = "deve ter de 2 a 150 caracteres") String name,
        @Size(min = 2, max = 150, message = "deve ter de 2 a 150 caracteres") String tradeName,
        @Size(max = 254, message = "deve ter no máximo 254 caracteres") String email,
        @Size(max = 16, message = "deve ter no máximo 16 caracteres") String phone
) {
}
