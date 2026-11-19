package com.crm.iam.domain.vos;

import com.crm.common.domain.exceptions.DomainException;
import com.crm.common.domain.exceptions.ValidationException;
import com.crm.common.domain.vos.StringValueObject;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import java.util.regex.Pattern;

/**
 * Segredo de 256 bits de um link de uso único, em base64 url-safe sem preenchimento (43
 * caracteres).
 *
 * <p>É um segredo: {@link #toString()} é mascarado para ele não escapar em log, e só o
 * {@link #hash()} chega ao banco.
 */
public final class TokenSecret extends StringValueObject {

    private static final int BYTES = 32;

    private static final Pattern FORMAT = Pattern.compile("^[A-Za-z0-9_-]{43}$");

    private static final SecureRandom RANDOM = new SecureRandom();

    private TokenSecret(String value) {
        super(value);
    }

    public static TokenSecret generate() {
        var bytes = new byte[BYTES];

        RANDOM.nextBytes(bytes);

        return new TokenSecret(Base64.getUrlEncoder().withoutPadding().encodeToString(bytes));
    }

    public static TokenSecret of(String value) {
        return new TokenSecret(value);
    }

    /**
     * Hash que o banco guarda no lugar do segredo.
     */
    public TokenHash hash() {
        try {
            var digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));

            return TokenHash.of(HexFormat.of().formatHex(digest));
        }
        catch (NoSuchAlgorithmException e) {
            // todo JDK é obrigado a ter SHA-256; o domínio não enxerga a exception de application
            throw new DomainException("algoritmo SHA-256 indisponível", e);
        }
    }

    @Override
    protected String type() {
        return "segredo do link";
    }

    @Override
    protected void customValidate(String value) {
        if (!FORMAT.matcher(value).matches())
            throw new ValidationException("formato de segredo do link inválido");
    }

    @Override
    public String toString() {
        return "***";
    }
}
