package com.crm.web.iam.requests;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Corpo de {@code PUT /auth/password}: as duas senhas, sem aparar. */
public record ChangePasswordRequest(
        @NotNull(message = "é obrigatório") @Size(max = 200, message = "é longo demais") String currentPassword,
        @NotNull(message = "é obrigatório") @Size(max = 200, message = "é longo demais") String newPassword
) {
    /** Sem nenhuma das senhas. */
    @Override
    public String toString() {
        return "ChangePasswordRequest[]";
    }
}
