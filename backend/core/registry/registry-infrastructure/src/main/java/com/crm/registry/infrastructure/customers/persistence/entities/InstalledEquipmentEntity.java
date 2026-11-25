package com.crm.registry.infrastructure.customers.persistence.entities;

import com.crm.common.infrastructure.persistence.entities.JpaEntity;
import com.crm.registry.domain.customers.enums.CustomerStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Linha da tabela {@code installed_equipment}. A unicidade do número de série por modelo, sem
 * diferenciar caixa, é o índice {@code uk_installed_equipment_model_serial}.
 */
@Entity
@Table(name = "installed_equipment")
@Getter
@Setter
@NoArgsConstructor
public class InstalledEquipmentEntity extends JpaEntity<UUID> {

    @Column(nullable = false)
    private UUID locationId;

    @Column(nullable = false)
    private UUID modelId;

    @Column(nullable = false)
    private String serialNumber;

    private LocalDate installedOn;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CustomerStatus status;
}
