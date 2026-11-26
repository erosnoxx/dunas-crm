package com.crm.registry.domain.customers.vos;

import com.crm.common.domain.vos.StringValueObject;

/**
 * Nome de um cliente, de 2 a 150 caracteres.
 */
public final class CustomerName extends StringValueObject {

    private static final int MIN_LENGTH = 2;
    private static final int MAX_LENGTH = 150;

    private CustomerName(String value) {
        super(value);
    }

    public static CustomerName of(String value) {
        return new CustomerName(value);
    }

    @Override
    protected String type() {
        return "nome do cliente";
    }

    @Override
    protected void customValidate(String value) {
        checkLength(value, MIN_LENGTH, MAX_LENGTH);
    }
}
