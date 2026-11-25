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

import java.util.UUID;

/**
 * Linha da tabela {@code customers}. Guarda valores crus: o domínio não tem anotação JPA. A
 * unicidade do documento é o índice {@code uk_customers_document}.
 */
@Entity
@Table(name = "customers")
@Getter
@Setter
@NoArgsConstructor
public class CustomerEntity extends JpaEntity<UUID> {

    @Column(nullable = false)
    private String document;

    @Column(nullable = false)
    private String name;

    private String tradeName;

    private String email;

    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CustomerStatus status;
}
