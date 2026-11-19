package com.crm.common.domain.vos;


import com.crm.common.domain.exceptions.ValidationException;

import java.time.Instant;

/**
 * Um período com início e fim. Estrutural, sem significado de negócio — é por isso que mora em
 * {@code common-domain} e não em {@code shared-domain}: "um intervalo de tempo" faz sentido em
 * qualquer sistema, não só neste produto.
 *
 * <p>{@code record}, não classe com getters manuais: a igualdade por valor e a imutabilidade já
 * vêm de graça, e o construtor compacto é o lugar certo para a invariante.
 */
public record TimeRange(Instant start, Instant end) {

    public TimeRange {
        if (start == null || end == null)
            throw new ValidationException("período não pode ter início ou fim nulos");

        if (!end.isAfter(start))
            throw new ValidationException("fim do período deve ser depois do início");
    }

    public static TimeRange of(Instant start, Instant end) {
        return new TimeRange(start, end);
    }

    public boolean contains(Instant instant) {
        if (instant == null)
            throw new ValidationException("instante não pode ser nulo");

        return !instant.isBefore(start) && !instant.isAfter(end);
    }
}
