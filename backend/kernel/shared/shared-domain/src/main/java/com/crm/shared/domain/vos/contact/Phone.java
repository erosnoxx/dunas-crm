package com.crm.shared.domain.vos.contact;

import com.crm.common.domain.exceptions.ValidationException;
import com.crm.common.domain.vos.StringValueObject;

import java.util.regex.Pattern;

/**
 * Telefone em formato E.164 — com o "+" e o código do país, sem separadores.
 *
 * <p>Guardar sempre normalizado evita a comparação impossível entre {@code (11) 99999-9999} e
 * {@code +5511999999999}, e é o formato que os provedores de SMS e WhatsApp esperam.
 *
 * <p>A formatação para leitura humana é responsabilidade da apresentação, não do domínio.
 */
public final class Phone extends StringValueObject {

    private static final Pattern FORMAT = Pattern.compile("^\\+[1-9]\\d{6,14}$");

    private Phone(String value) {
        super(value);
    }

    public static Phone of(String value) {
        return new Phone(value);
    }

    @Override
    protected String type() {
        return "telefone";
    }

    @Override
    protected void customValidate(String value) {
        if (!FORMAT.matcher(value).matches())
            throw new ValidationException("formato de telefone inválido — use E.164 (ex: +5511999999999)");
    }
}
