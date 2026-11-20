package com.crm.web.iam.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Corpo de {@code POST /auth/password/forgot}. */
public record ForgotPasswordRequest(
        @NotBlank(message = "é obrigatório") @Size(max = 254, message = "é longo demais") String email
) {
}
