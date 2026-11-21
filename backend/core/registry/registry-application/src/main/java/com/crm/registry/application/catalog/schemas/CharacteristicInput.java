package com.crm.registry.application.catalog.schemas;

/**
 * Característica do tipo como chega da requisição: ainda texto, o use case a interpreta.
 *
 * @param name     nome da característica
 * @param unit     nome exato de {@code MeasurementUnit}
 * @param required se o modelo precisa informá-la
 */
public record CharacteristicInput(
        String name,
        String unit,
        boolean required
) {
}
