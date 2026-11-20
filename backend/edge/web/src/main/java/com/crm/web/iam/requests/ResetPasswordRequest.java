package com.crm.web.iam.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Corpo de {@code POST /auth/password/reset}: o segredo do link e a senha nova. */
public record ResetPasswordRequest(
        @NotBlank(message = "é obrigatório") @Size(max = 200, message = "é longo demais") String token,
        @NotNull(message = "é obrigatório") @Size(max = 200, message = "é longo demais") String password
) {
    /** Sem o segredo do link nem a senha. */
    @Override
    public String toString() {
        return "ResetPasswordRequest[]";
    }
}
