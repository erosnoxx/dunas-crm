package com.crm.registry.infrastructure.catalog.persistence.entities;

import com.crm.common.infrastructure.persistence.entities.JpaEntity;
import com.crm.registry.domain.catalog.enums.CatalogStatus;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Linha da tabela {@code equipment_models}, com os valores da característica. Fabricante e tipo
 * ficam por id. A unicidade do nome por fabricante, sem diferenciar caixa, é o índice
 * {@code uk_equipment_models_manufacturer_name}.
 */
@Entity
@Table(name = "equipment_models")
@Getter
@Setter
@NoArgsConstructor
public class EquipmentModelEntity extends JpaEntity<UUID> {

    @Column(nullable = false)
    private UUID manufacturerId;

    @Column(nullable = false)
    private UUID typeId;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CatalogStatus status;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "model_characteristic_values", joinColumns = @JoinColumn(name = "model_id"))
    private List<ModelCharacteristicValueEntity> values = new ArrayList<>();
}
