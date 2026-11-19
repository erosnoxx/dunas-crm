package com.crm.common.domain.vos;

import com.crm.common.domain.exceptions.ValidationException;
import lombok.Getter;
import java.util.Objects;

/**
 * Base de todo Value Object: imutável, nunca nulo, validado na construção e igual por valor.
 *
 * <p>Valor nulo é recusado aqui, para todos os VOs, com {@link ValidationException} (400): é entrada
 * ausente, a mesma categoria de texto vazio ou formato inválido, e não regra de negócio (422).
 *
 * <p>{@link #validate(Object)} roda dentro do construtor, antes de os campos da subclasse existirem;
 * regras que dependem de outro dado devem ser checadas no construtor da subclasse.
 *
 * @param <T> tipo do valor embrulhado
 */
@Getter
public abstract class ValueObject<T> {
    protected final T value;

    protected ValueObject(T value) {
        if (value == null) {
            throw new ValidationException("o valor não pode ser nulo");
        }

        this.value = validate(value);
    }

    protected abstract T validate(T value);

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        ValueObject<?> that = (ValueObject<?>) obj;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return value != null ? value.toString() : "null";
    }
}
