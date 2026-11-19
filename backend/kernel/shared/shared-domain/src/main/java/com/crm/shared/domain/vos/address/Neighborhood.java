package com.crm.shared.domain.vos.address;

import com.crm.common.domain.vos.StringValueObject;

/**
 * Nome de bairro, de 2 a 100 caracteres.
 */
public final class Neighborhood extends StringValueObject {

    private static final int MIN_LENGTH = 2;
    private static final int MAX_LENGTH = 100;

    private Neighborhood(String value) {
        super(value);
    }

    public static Neighborhood of(String value) {
        return new Neighborhood(value);
    }

    @Override
    protected String type() {
        return "bairro";
    }

    @Override
    protected void customValidate(String value) {
        checkLength(value, MIN_LENGTH, MAX_LENGTH);
    }
}
