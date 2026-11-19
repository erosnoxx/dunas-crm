package com.crm.shared.domain.vos.address;


import com.crm.common.domain.vos.StringValueObject;

/**
 * Complemento de endereço (bloco, apartamento, sala). Ao contrário dos demais componentes de
 * endereço, é tipicamente opcional a nível de agregado — mas esse VO, como qualquer outro,
 * representa um valor que existe: "opcional" é modelado como ausência da instância (ex.
 * {@code Optional<AddressComplement>} no composto), nunca como um {@code AddressComplement} que
 * aceita vazio.
 */
public final class AddressComplement extends StringValueObject {

    private static final int MAX_LENGTH = 50;

    private AddressComplement(String value) {
        super(value);
    }

    public static AddressComplement of(String value) {
        return new AddressComplement(value);
    }

    @Override
    protected String type() {
        return "complemento do endereço";
    }

    @Override
    protected void customValidate(String value) {
        checkLength(value, 1, MAX_LENGTH);
    }
}
