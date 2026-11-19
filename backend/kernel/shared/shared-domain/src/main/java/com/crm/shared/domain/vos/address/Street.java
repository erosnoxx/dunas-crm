package com.crm.shared.domain.vos.address;

import com.crm.common.domain.vos.StringValueObject;

/**
 * Logradouro (nome da rua/avenida), sem número.
 *
 * <p>No projeto anterior este VO se chamava {@code AddressValue} — nome que não dizia se era o
 * logradouro, o endereço inteiro ou outra coisa. Renomeado para o que ele de fato é.
 */
public final class Street extends StringValueObject {

    private static final int MIN_LENGTH = 2;
    private static final int MAX_LENGTH = 150;

    private Street(String value) {
        super(value);
    }

    public static Street of(String value) {
        return new Street(value);
    }

    @Override
    protected String type() {
        return "logradouro";
    }

    @Override
    protected void customValidate(String value) {
        checkLength(value, MIN_LENGTH, MAX_LENGTH);
    }
}
