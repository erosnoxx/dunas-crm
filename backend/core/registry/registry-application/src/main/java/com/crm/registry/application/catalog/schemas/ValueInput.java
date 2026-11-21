package com.crm.registry.application.catalog.schemas;

import java.util.UUID;

/**
 * Valor do modelo como chega da requisição: o número ainda é texto, o use case o interpreta.
 *
 * @param definitionId definição da característica do tipo
 * @param amount       valor, maior que zero e com até 3 casas decimais
 */
public record ValueInput(
        UUID definitionId,
        String amount
) {
}
