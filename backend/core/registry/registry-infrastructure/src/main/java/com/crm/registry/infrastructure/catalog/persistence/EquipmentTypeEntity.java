package com.crm.registry.infrastructure.catalog.persistence;

import com.crm.common.infrastructure.persistence.entities.JpaEntity;
import com.crm.registry.domain.catalog.vos.CatalogStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Linha da tabela {@code equipment_types}, com a definição da característica. A definição é
 * carregada junto (o aggregate vem sempre inteiro) e gravada em cascata com o tipo.
 */
@Entity
@Table(name = "equipment_types")
@Getter
@Setter
@NoArgsConstructor
public class EquipmentTypeEntity extends JpaEntity<UUID> {

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CatalogStatus status;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "type_id", nullable = false)
    private List<CharacteristicDefinitionEntity> characteristics = new ArrayList<>();
}
