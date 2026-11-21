package com.crm.registry.infrastructure.catalog.seed;

import java.util.List;

/**
 * Um item de {@code seed/catalog.json}: a carga inicial do catálogo, com os fabricantes, os tipos e
 * os modelos. Modelo referencia fabricante e tipo pelo <b>nome</b>, e só pode citar o que está na
 * mesma carga. A característica do modelo é a única do tipo, então o modelo traz só o valor.
 *
 * <p>Lista ausente no JSON vale vazia.
 *
 * @param manufacturers fabricantes a cadastrar
 * @param types         tipos a cadastrar, cada um com a definição da sua característica
 * @param models        modelos a cadastrar
 */
public record CatalogSeed(
        List<ManufacturerSeed> manufacturers,
        List<TypeSeed> types,
        List<ModelSeed> models
) {

    public CatalogSeed {
        manufacturers = manufacturers == null ? List.of() : manufacturers;
        types = types == null ? List.of() : types;
        models = models == null ? List.of() : models;
    }

    /** @param name nome do fabricante */
    public record ManufacturerSeed(String name) {
    }

    /**
     * @param name               nome do tipo
     * @param characteristicName nome da característica do tipo (ex. Potência)
     * @param unit               nome exato da {@code MeasurementUnit} (ex. KILOWATT)
     * @param required           se o modelo precisa informar a característica
     */
    public record TypeSeed(
            String name,
            String characteristicName,
            String unit,
            boolean required
    ) {
    }

    /**
     * @param manufacturer nome de um fabricante da mesma carga
     * @param type         nome de um tipo da mesma carga
     * @param name         nome do modelo
     * @param amount       valor da característica do tipo, em texto (ex. "3")
     */
    public record ModelSeed(
            String manufacturer,
            String type,
            String name,
            String amount
    ) {
    }
}
