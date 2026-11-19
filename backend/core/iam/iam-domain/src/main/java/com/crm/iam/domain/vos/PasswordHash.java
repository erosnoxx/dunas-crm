package com.crm.iam.domain.vos;

import com.crm.common.domain.exceptions.ValidationException;
import com.crm.common.domain.vos.StringValueObject;

/**
 * Hash Argon2id de uma senha, já calculado (formato PHC). O domínio só o guarda e o compara por
 * valor: calcular e conferir o hash é do {@code PasswordHasher}, que depende de biblioteca.
 */
public final class PasswordHash extends StringValueObject {

    private static final String PREFIX = "$argon2id$";

    private PasswordHash(String value) {
        super(value);
    }

    public static PasswordHash of(String value) {
        return new PasswordHash(value);
    }

    @Override
    protected String type() {
        return "hash da senha";
    }

    @Override
    protected void customValidate(String value) {
        if (!value.startsWith(PREFIX))
            throw new ValidationException("formato de hash da senha inválido");
    }
}
