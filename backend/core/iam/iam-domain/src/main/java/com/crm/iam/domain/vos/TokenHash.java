package com.crm.iam.domain.vos;

import com.crm.common.domain.exceptions.ValidationException;
import com.crm.common.domain.vos.StringValueObject;

import java.util.regex.Pattern;

/**
 * SHA-256 de um {@link TokenSecret}, em hexadecimal minúsculo (64 caracteres). É o que o banco
 * guarda: o segredo em si só existe no link enviado, então um vazamento da tabela não entrega
 * links válidos.
 */
public final class TokenHash extends StringValueObject {

    private static final Pattern FORMAT = Pattern.compile("^[0-9a-f]{64}$");

    private TokenHash(String value) {
        super(value);
    }

    public static TokenHash of(String value) {
        return new TokenHash(value);
    }

    @Override
    protected String type() {
        return "hash do token";
    }

    @Override
    protected void customValidate(String value) {
        if (!FORMAT.matcher(value).matches())
            throw new ValidationException("formato de hash do token inválido");
    }
}
