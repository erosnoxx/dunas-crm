package com.crm.registry.domain.customers.vos;

import com.crm.common.domain.vos.StringValueObject;

/**
 * Número de série de um equipamento, com a caixa como digitada, de 1 a 60 caracteres.
 */
public final class SerialNumber extends StringValueObject {

    private static final int MIN_LENGTH = 1;
    private static final int MAX_LENGTH = 60;

    private SerialNumber(String value) {
        super(value);
    }

    public static SerialNumber of(String value) {
        return new SerialNumber(value);
    }

    @Override
    protected String type() {
        return "número de série";
    }

    @Override
    protected void customValidate(String value) {
        checkLength(value, MIN_LENGTH, MAX_LENGTH);
    }
}
