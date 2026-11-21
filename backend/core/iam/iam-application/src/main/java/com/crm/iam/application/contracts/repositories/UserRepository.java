package com.crm.iam.application.contracts.repositories;

import com.crm.common.application.repository.Repository;
import com.crm.iam.domain.entities.User;
import com.crm.shared.domain.vos.contact.Email;

import java.util.Optional;

/** Persistência do aggregate {@link User}. */
public interface UserRepository extends Repository<User, UserCriteria> {

    /** Busca pelo e-mail, que já chega normalizado pelo {@link Email}. */
    Optional<User> findByEmail(Email email);
}
