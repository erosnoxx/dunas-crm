package com.crm.shared.domain.vos.address;

import com.crm.common.domain.vos.StringValueObject;

/**
 * CEP, guardado só com os 8 dígitos — sem hífen.
 *
 * <p>Aceita qualquer máscara na entrada ({@code of} remove tudo que não é dígito antes de
 * validar) e expõe {@link #formatted()} para exibição. Guardar sem máscara é o que torna a
 * comparação e a busca por CEP triviais.
 */
public final class ZipCode extends StringValueObject {

    private static final int LENGTH = 8;

    private ZipCode(String value) {
        super(value);
    }

    public static ZipCode of(String value) {
        return new ZipCode(value == null ? null : value.replaceAll("\\D", ""));
    }

    public String formatted() {
        var v = getValue();
        return v.substring(0, 5) + "-" + v.substring(5);
    }

    @Override
    protected String type() {
        return "cep";
    }

    @Override
    protected void customValidate(String value) {
        checkLength(value, LENGTH, LENGTH);
    }
}
