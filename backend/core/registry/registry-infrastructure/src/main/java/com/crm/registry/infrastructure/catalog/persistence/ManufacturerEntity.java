package com.crm.registry.infrastructure.catalog.persistence;

import com.crm.common.infrastructure.persistence.entities.JpaEntity;
import com.crm.registry.domain.catalog.vos.CatalogStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * Linha da tabela {@code manufacturers}. Guarda valores crus: o domínio não tem anotação JPA. A
 * unicidade do nome sem diferenciar caixa é o índice {@code uk_manufacturers_name}.
 */
@Entity
@Table(name = "manufacturers")
@Getter
@Setter
@NoArgsConstructor
public class ManufacturerEntity extends JpaEntity<UUID> {

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CatalogStatus status;
}
