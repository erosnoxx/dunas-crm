package com.crm.iam.domain.vos;

import com.crm.common.domain.vos.StringValueObject;

/**
 * Rótulo do aparelho de uma sessão (ex. "Galaxy A54, app DUNAS"), de 1 a 80 caracteres. Serve só
 * para o usuário reconhecer a própria sessão na lista.
 */
public final class DeviceLabel extends StringValueObject {

    private static final int MIN_LENGTH = 1;
    private static final int MAX_LENGTH = 80;

    private DeviceLabel(String value) {
        super(value);
    }

    public static DeviceLabel of(String value) {
        return new DeviceLabel(value);
    }

    @Override
    protected String type() {
        return "rótulo do aparelho";
    }

    @Override
    protected void customValidate(String value) {
        checkLength(value, MIN_LENGTH, MAX_LENGTH);
    }
}
