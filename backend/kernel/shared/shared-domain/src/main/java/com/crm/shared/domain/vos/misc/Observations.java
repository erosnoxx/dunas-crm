package com.crm.shared.domain.vos.misc;

import com.crm.common.domain.vos.StringValueObject;

/**
 * Texto livre de observações, de 1 a {@value #MAX_LENGTH} caracteres.
 */
public final class Observations extends StringValueObject {
    public static final int MAX_LENGTH = 500;

    private Observations(String value) {
        super(value);
    }

    public static Observations of(String value) {
        return new Observations(value);
    }

    @Override
    protected String type() {
        return "observações";
    }

    @Override
    protected void customValidate(String value) {
        checkLength(value, 1, MAX_LENGTH);
    }
}
