package com.crm.iam.infrastructure.persistence;

import com.crm.common.infrastructure.persistence.entities.JpaEntity;
import com.crm.iam.domain.vos.Role;
import com.crm.iam.domain.vos.UserStatus;
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
 * Linha da tabela {@code users}. Guarda valores crus (texto, enum): o domínio não tem anotação
 * JPA. Sem {@code toString} gerado, para o hash da senha nunca cair num log.
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
public class UserEntity extends JpaEntity<UUID> {

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserStatus status;

    private String passwordHash;

    @Column(nullable = false)
    private boolean mustChangePassword;
}
