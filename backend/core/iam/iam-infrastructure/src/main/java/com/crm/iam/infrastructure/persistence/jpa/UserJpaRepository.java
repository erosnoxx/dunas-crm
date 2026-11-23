package com.crm.iam.infrastructure.persistence.jpa;

import com.crm.iam.infrastructure.persistence.entities.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/** Spring Data de {@link UserEntity}; só a infraestrutura o enxerga. */
public interface UserJpaRepository extends JpaRepository<UserEntity, UUID> {

    Optional<UserEntity> findByEmail(String email);
}
