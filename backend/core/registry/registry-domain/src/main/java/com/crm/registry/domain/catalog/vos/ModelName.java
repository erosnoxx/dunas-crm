package com.crm.registry.domain.catalog.vos;

import com.crm.common.domain.vos.StringValueObject;

/**
 * Nome de um modelo de equipamento, de 1 a 80 caracteres.
 */
public final class ModelName extends StringValueObject {

    private static final int MIN_LENGTH = 1;
    private static final int MAX_LENGTH = 80;

    private ModelName(String value) {
        super(value);
    }

    public static ModelName of(String value) {
        return new ModelName(value);
    }

    @Override
    protected String type() {
        return "nome do modelo";
    }

    @Override
    protected void customValidate(String value) {
        checkLength(value, MIN_LENGTH, MAX_LENGTH);
    }
}
