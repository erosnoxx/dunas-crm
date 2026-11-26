package com.crm.iam.application.schemas;

import java.time.Instant;
import java.util.UUID;

/**
 * Aparelho conectado, como a tela de sessões o mostra.
 *
 * @param current se é a sessão de quem está consultando
 */
public record SessionSummary(
        UUID id,
        String device,
        Instant startedAt,
        Instant lastUsedAt,
        boolean current
) {
}
