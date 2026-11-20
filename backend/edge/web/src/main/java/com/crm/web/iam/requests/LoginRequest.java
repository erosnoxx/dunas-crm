package com.crm.web.iam.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Corpo de {@code POST /auth/login}.
 *
 * <p>A senha não é aparada nem tem regra de formato aqui: o limite é só contra abuso, e a regra
 * real é do domínio.
 */
public record LoginRequest(
        @NotBlank(message = "é obrigatório") @Size(max = 254, message = "é longo demais") String email,
        @NotNull(message = "é obrigatório") @Size(max = 200, message = "é longo demais") String password
) {
    /** Sem a senha: o log e o {@code toString()} não podem vazá-la. */
    @Override
    public String toString() {
        return "LoginRequest[email=" + email + "]";
    }
}
