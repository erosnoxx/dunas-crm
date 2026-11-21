package com.crm.registry.domain.catalog.vos;

import com.crm.common.domain.vos.StringValueObject;

/**
 * Nome de um tipo de equipamento, de 2 a 60 caracteres.
 */
public final class TypeName extends StringValueObject {

    private static final int MIN_LENGTH = 2;
    private static final int MAX_LENGTH = 60;

    private TypeName(String value) {
        super(value);
    }

    public static TypeName of(String value) {
        return new TypeName(value);
    }

    @Override
    protected String type() {
        return "nome do tipo";
    }

    @Override
    protected void customValidate(String value) {
        checkLength(value, MIN_LENGTH, MAX_LENGTH);
    }
}
