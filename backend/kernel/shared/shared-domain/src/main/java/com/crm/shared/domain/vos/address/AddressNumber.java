package com.crm.shared.domain.vos.address;

import com.crm.common.domain.vos.StringValueObject;

/**
 * Número do endereço. É texto, não número — precisa caber "S/N" (sem número) e complemento
 * embutido tipo "123A".
 */
public final class AddressNumber extends StringValueObject {

    private static final int MAX_LENGTH = 15;

    private AddressNumber(String value) {
        super(value);
    }

    public static AddressNumber of(String value) {
        return new AddressNumber(value == null ? null : value.toUpperCase());
    }

    @Override
    protected String type() {
        return "número do endereço";
    }

    @Override
    protected void customValidate(String value) {
        checkLength(value, 1, MAX_LENGTH);
    }
}
