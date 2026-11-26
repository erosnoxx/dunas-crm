package com.crm.web.iam.requests;

import jakarta.validation.constraints.NotBlank;

/** Corpo de {@code PUT /users/{id}/role}. */
public record ChangeRoleRequest(
        @NotBlank(message = "é obrigatório") String role
) {
}
