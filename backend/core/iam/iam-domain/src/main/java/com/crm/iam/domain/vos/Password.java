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

    /**
     * Diz se {@link #of} aceitaria o texto, sem lançar; nulo é inválido. Serve a quem precisa tratar
     * uma senha fora da regra como credencial errada, sem revelar a regra a quem não tem conta.
     */
    public static boolean isValid(String value) {
        return value != null && violation(value) == null;
    }

    @Override
    protected String validate(String value) {
        var violation = violation(value);

        if (violation != null)
            throw reject(violation);

        return value;
    }

    /** Primeiro critério que o texto não cumpre, ou nulo se cumpre todos: a regra mora só aqui. */
    private static String violation(String value) {
        if (value.length() < MIN_LENGTH)
            return "no mínimo " + MIN_LENGTH + " caracteres";

        if (value.length() > MAX_LENGTH)
            return "no máximo " + MAX_LENGTH + " caracteres";

        if (value.chars().noneMatch(Character::isUpperCase))
            return "ao menos uma letra maiúscula";

        if (value.chars().noneMatch(Character::isLowerCase))
            return "ao menos uma letra minúscula";

        if (value.chars().noneMatch(Character::isDigit))
            return "ao menos um número";

        if (value.chars().noneMatch(Password::isSpecial))
            return "ao menos um caractere especial";

        return null;
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
