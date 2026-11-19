package com.crm.common.domain.vos;


import com.crm.common.domain.exceptions.ValidationException;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * URL absoluta, HTTP ou HTTPS. Validação de formato pura — zero significado de negócio, por isso
 * mora em {@code common-domain}.
 *
 * <p>Nome em {@code PascalCase} normal (não {@code URL}, como no projeto anterior) para não
 * colidir visualmente com {@link java.net.URL} do próprio JDK — que, aliás, este VO
 * deliberadamente não usa: {@link java.net.URL} pode disparar resolução de DNS em alguns
 * construtores/comparações. {@link URI} é puramente sintático.
 */
public final class Url extends StringValueObject {

    private static final int MAX_LENGTH = 2048;

    private Url(String value) {
        super(value);
    }

    public static Url of(String value) {
        return new Url(value);
    }

    @Override
    protected String type() {
        return "url";
    }

    @Override
    protected void customValidate(String value) {
        checkLength(value, 1, MAX_LENGTH);

        if (value.chars().anyMatch(Character::isWhitespace))
            throw new ValidationException("url não pode conter espaços em branco");

        final URI uri;

        try {
            uri = new URI(value);
        } catch (URISyntaxException e) {
            throw new ValidationException("url inválida");
        }

        if (!uri.isAbsolute())
            throw new ValidationException("url deve ser absoluta");

        if (uri.getScheme() == null)
            throw new ValidationException("url deve possuir um esquema");

        if (!uri.getScheme().equalsIgnoreCase("http") && !uri.getScheme().equalsIgnoreCase("https"))
            throw new ValidationException("url deve utilizar http ou https");

        if (uri.getHost() == null)
            throw new ValidationException("url deve possuir um host válido");
    }
}
