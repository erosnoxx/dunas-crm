package com.crm.registry.infrastructure.catalog.persistence.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Linha da tabela {@code model_characteristic_values}. É value object do modelo, então é
 * {@link Embeddable}: sem id, versão nem datas próprias, e trocar um valor muda a coleção do modelo,
 * o que sobe a versão dele.
 */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ModelCharacteristicValueEntity {

    @Column(name = "definition_id", nullable = false)
    private UUID definitionId;

    @Column(nullable = false, precision = 12, scale = 3)
    private BigDecimal amount;
}
