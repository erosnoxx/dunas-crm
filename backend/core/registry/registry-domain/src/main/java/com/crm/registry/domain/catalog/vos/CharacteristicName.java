package com.crm.registry.domain.catalog.vos;

import com.crm.common.domain.vos.StringValueObject;

/**
 * Nome de uma característica de um tipo de equipamento, de 2 a 40 caracteres.
 */
public final class CharacteristicName extends StringValueObject {

    private static final int MIN_LENGTH = 2;
    private static final int MAX_LENGTH = 40;

    private CharacteristicName(String value) {
        super(value);
    }

    public static CharacteristicName of(String value) {
        return new CharacteristicName(value);
    }

    @Override
    protected String type() {
        return "nome da característica";
    }

    @Override
    protected void customValidate(String value) {
        checkLength(value, MIN_LENGTH, MAX_LENGTH);
    }
}
