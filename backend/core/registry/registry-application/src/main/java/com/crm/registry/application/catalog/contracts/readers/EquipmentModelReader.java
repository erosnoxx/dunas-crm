package com.crm.registry.application.catalog.contracts.readers;

import java.util.UUID;

/** Leitura de modelos de equipamento, sem carregar o aggregate. */
public interface EquipmentModelReader {

    /** Diz se algum modelo (ativo ou não) usa o tipo; é o que trava a característica (ADR-4). */
    boolean hasModelsOfType(UUID typeId);
}
