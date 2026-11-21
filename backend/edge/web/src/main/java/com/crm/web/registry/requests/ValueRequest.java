package com.crm.web.registry.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/** Valor de uma característica do modelo. O {@code amount} é texto: o use case o converte e recusa o inválido. */
public record ValueRequest(
        @NotNull(message = "é obrigatório") UUID definitionId,
        @NotBlank(message = "é obrigatório") String amount
) {
}
