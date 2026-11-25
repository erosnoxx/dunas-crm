package com.crm.registry.infrastructure.integrations.brasilapi.dtos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** A resposta de {@code GET /api/cep/v2/{cep}} da BrasilAPI. Só o que o Translator lê. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record BrasilApiCepResponse(
        String cep,
        String state,
        String city,
        String neighborhood,
        String street
) {
}
