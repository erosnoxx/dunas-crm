package com.crm.web.iam.requests;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Corpo de {@code POST /users}. O papel é validado pelo use case, que conhece os valores. */
public record InviteUserRequest(
        @NotBlank(message = "é obrigatório") @Size(max = 200, message = "é longo demais") String name,
        @NotBlank(message = "é obrigatório") @Email(message = "e-mail inválido") @Size(max = 254, message = "é longo demais") String email,
        @NotBlank(message = "é obrigatório") String role
) {
}
