package com.crm.shared.domain.vos.address;

import com.crm.common.domain.vos.StringValueObject;

/**
 * Nome de cidade, de 1 a 100 caracteres.
 */
public final class CityName extends StringValueObject {

    private static final int MAX_LENGTH = 100;

    private CityName(String value) {
        super(value);
    }

    public static CityName of(String value) {
        return new CityName(value);
    }

    @Override
    protected String type() {
        return "cidade";
    }

    @Override
    protected void customValidate(String value) {
        checkLength(value, 1, MAX_LENGTH);
    }
}
