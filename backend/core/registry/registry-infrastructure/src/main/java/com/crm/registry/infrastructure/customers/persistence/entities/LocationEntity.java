package com.crm.registry.infrastructure.customers.persistence.entities;

import com.crm.common.infrastructure.persistence.entities.JpaEntity;
import com.crm.registry.domain.customers.enums.CustomerStatus;
import com.crm.shared.domain.enums.UF;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

/**
 * Linha da tabela {@code locations}. O endereço, que é value object do local, vive nas colunas
 * da própria linha; não há tabela filha.
 */
@Entity
@Table(name = "locations")
@Getter
@Setter
@NoArgsConstructor
public class LocationEntity extends JpaEntity<UUID> {

    @Column(nullable = false)
    private UUID customerId;

    @Column(nullable = false)
    private String name;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(nullable = false, length = 8)
    private String zipCode;

    @Column(nullable = false)
    private String street;

    @Column(nullable = false)
    private String number;

    private String complement;

    @Column(nullable = false)
    private String neighborhood;

    @Column(nullable = false)
    private String city;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(nullable = false, length = 2)
    private UF state;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CustomerStatus status;
}
