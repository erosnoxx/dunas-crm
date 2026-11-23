package com.crm.iam.infrastructure.repositories;

import com.crm.common.infrastructure.persistence.repositories.RepositoryImpl;
import com.crm.iam.application.criteria.UserCriteria;
import com.crm.iam.application.contracts.repositories.UserRepository;
import com.crm.iam.domain.entities.User;
import com.crm.iam.infrastructure.mappers.UserMapper;
import com.crm.iam.infrastructure.persistence.entities.UserEntity;
import com.crm.iam.infrastructure.persistence.jpa.UserJpaRepository;
import com.crm.shared.domain.vos.contact.Email;
import jakarta.persistence.EntityManager;

import java.util.Optional;

/**
 * Repository de {@link User} sobre JPA. A unicidade do e-mail é a constraint do banco: o e-mail
 * chega normalizado pelo {@link Email}, então caixa diferente cai na mesma chave e o save
 * devolve {@code ConflictException}.
 */
public class UserRepositoryImpl
        extends RepositoryImpl<User, UserCriteria, UserEntity, UserJpaRepository>
        implements UserRepository {

    public UserRepositoryImpl(
            UserJpaRepository jpaRepository,
            EntityManager em,
            UserMapper mapper
    ) {
        super(
                jpaRepository,
                em,
                mapper,
                UserEntity.class
        );
    }

    @Override
    public Optional<User> findByEmail(Email email) {
        return jpaRepository.findByEmail(email.getValue())
                .map(mapper::toDomain);
    }
}
