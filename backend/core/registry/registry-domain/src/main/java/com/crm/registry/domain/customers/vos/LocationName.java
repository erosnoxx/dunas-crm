package com.crm.registry.domain.customers.vos;

import com.crm.common.domain.vos.StringValueObject;

/**
 * Nome de um local de instalação, de 2 a 60 caracteres.
 */
public final class LocationName extends StringValueObject {

    private static final int MIN_LENGTH = 2;
    private static final int MAX_LENGTH = 60;

    private LocationName(String value) {
        super(value);
    }

    public static LocationName of(String value) {
        return new LocationName(value);
    }

    @Override
    protected String type() {
        return "nome do local";
    }

    @Override
    protected void customValidate(String value) {
        checkLength(value, MIN_LENGTH, MAX_LENGTH);
    }
}
