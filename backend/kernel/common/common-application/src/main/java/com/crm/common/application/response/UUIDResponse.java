package com.crm.common.application.response;

import java.util.UUID;

/** Resposta padrão de criação de aggregate: só o id gerado ({@code {"id": "..."}}). */
public record UUIDResponse(UUID id) {
}
