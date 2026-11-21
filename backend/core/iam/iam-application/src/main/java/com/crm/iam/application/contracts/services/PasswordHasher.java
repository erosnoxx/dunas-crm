package com.crm.iam.application.contracts.services;

import com.crm.iam.domain.vos.Password;
import com.crm.iam.domain.vos.PasswordHash;

/** Hash e conferência de senha; a infrastructure implementa com Argon2id. */
public interface PasswordHasher {

    PasswordHash hash(Password password);

    boolean matches(
            Password password,
            PasswordHash hash
    );
}
