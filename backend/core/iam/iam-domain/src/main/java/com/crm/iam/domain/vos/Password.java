package com.crm.iam.domain.vos;

import com.crm.common.domain.exceptions.ValidationException;
import com.crm.common.domain.vos.ValueObject;

/**
 * Senha em texto, só em trânsito: de 8 a 30 caracteres, com maiúscula, minúscula, número e
 * caractere especial (ADR-3). Nunca é guardada.
 *
 * <p>Não estende {@code StringValueObject} de propósito: aquele apara as pontas, e aparar altera a
 * senha. O espaço também não conta como caractere especial.
 *
 * <p>É um segredo: {@link #toString()} é mascarado e a igualdade é por identidade, para que nem
 * o texto nem um hash derivado dele escapem por log ou por coleção.
 */
public final class Password extends ValueObject<String> {

    private static final String CODE = "invalid_password";

    private static final int MIN_LENGTH = 8;
    private static final int MAX_LENGTH = 30;

    private Password(String value) {
        super(value);
    }

    public static Password of(String value) {
        return new Password(value);
    }

    @Override
    protected String validate(String value) {
        if (value.length() < MIN_LENGTH)
            throw reject("no mínimo " + MIN_LENGTH + " caracteres");

        if (value.length() > MAX_LENGTH)
            throw reject("no máximo " + MAX_LENGTH + " caracteres");

        if (value.chars().noneMatch(Character::isUpperCase))
            throw reject("ao menos uma letra maiúscula");

        if (value.chars().noneMatch(Character::isLowerCase))
            throw reject("ao menos uma letra minúscula");

        if (value.chars().noneMatch(Character::isDigit))
            throw reject("ao menos um número");

        if (value.chars().noneMatch(Password::isSpecial))
            throw reject("ao menos um caractere especial");

        return value;
    }

    private static boolean isSpecial(int codePoint) {
        return !Character.isLetterOrDigit(codePoint) && !Character.isWhitespace(codePoint);
    }

    private static ValidationException reject(String criterion) {
        return new ValidationException(CODE, "a senha precisa de " + criterion);
    }

    @Override
    public boolean equals(Object obj) {
        return this == obj;
    }

    @Override
    public int hashCode() {
        return System.identityHashCode(this);
    }

    @Override
    public String toString() {
        return "***";
    }
}
