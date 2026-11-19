package com.crm.iam.domain.vos;

import com.crm.common.domain.vos.StringValueObject;

/**
 * Nome de exibição de um usuário, de 2 a 80 caracteres.
 */
public final class UserName extends StringValueObject {

    private static final int MIN_LENGTH = 2;
    private static final int MAX_LENGTH = 80;

    private UserName(String value) {
        super(value);
    }

    public static UserName of(String value) {
        return new UserName(value);
    }

    @Override
    protected String type() {
        return "nome do usuário";
    }

    @Override
    protected void customValidate(String value) {
        checkLength(value, MIN_LENGTH, MAX_LENGTH);
    }
}
