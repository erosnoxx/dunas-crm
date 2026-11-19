package com.crm.shared.domain.vos.contact;

import com.crm.common.domain.exceptions.ValidationException;
import com.crm.common.domain.vos.StringValueObject;

import java.util.regex.Pattern;

/**
 * Endereço de e-mail, normalizado para minúsculas.
 *
 * <p>A normalização não é cosmética: o e-mail é a identidade de login e a chave de unicidade de
 * conta. Sem ela, {@code Joao@x.com} e {@code joao@x.com} criariam duas contas.
 *
 * <p>O limite de 254 caracteres é o máximo de um endereço segundo a RFC 5321.
 */
public final class Email extends StringValueObject {

    private static final int MAX_LENGTH = 254;

    private static final Pattern FORMAT = Pattern.compile(
            "^[a-zA-Z0-9._%+\\-]+@(?:[a-zA-Z0-9](?:[a-zA-Z0-9\\-]*[a-zA-Z0-9])?\\.)+[a-zA-Z]{2,}$"
    );

    private Email(String value) {
        super(value);
    }

    public static Email of(String value) {
        return new Email(value);
    }

    @Override
    protected String validate(String value) {
        return super.validate(value).toLowerCase();
    }

    @Override
    protected String type() {
        return "e-mail";
    }

    @Override
    protected void customValidate(String value) {
        checkLength(value, 1, MAX_LENGTH);

        if (!FORMAT.matcher(value).matches())
            throw new ValidationException("formato de e-mail inválido");
    }
}
