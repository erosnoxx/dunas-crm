package com.crm.registry.domain.catalog.vos;

import com.crm.common.domain.vos.StringValueObject;

/**
 * Nome de um fabricante de equipamento, de 2 a 80 caracteres.
 */
public final class ManufacturerName extends StringValueObject {

    private static final int MIN_LENGTH = 2;
    private static final int MAX_LENGTH = 80;

    private ManufacturerName(String value) {
        super(value);
    }

    public static ManufacturerName of(String value) {
        return new ManufacturerName(value);
    }

    @Override
    protected String type() {
        return "nome do fabricante";
    }

    @Override
    protected void customValidate(String value) {
        checkLength(value, MIN_LENGTH, MAX_LENGTH);
    }
}
