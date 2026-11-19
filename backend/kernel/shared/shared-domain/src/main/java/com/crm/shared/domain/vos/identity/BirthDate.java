package com.crm.shared.domain.vos.identity;

import com.crm.common.domain.exceptions.ValidationException;
import com.crm.common.domain.vos.LocalDateValueObject;

import java.time.Clock;
import java.time.LocalDate;

/**
 * Data de nascimento.
 *
 * <p>A única regra é não estar no futuro: hoje é válido (recém-nascido). Não há idade mínima nem
 * máxima aqui, porque isso depende do contexto de quem usa — um cadastro que exige maioridade
 * valida isso na própria regra, não neste valor.
 *
 * <p>"Hoje" vem do {@link Clock} recebido, nunca do relógio do sistema: o domínio não lê a hora
 * sozinho (ver {@code ClockRulesTest}), e é isso que deixa a regra determinística nos testes.
 */
public final class BirthDate extends LocalDateValueObject {

    private BirthDate(LocalDate value, LocalDate today) {
        super(value);

        if (value.isAfter(today))
            throw new ValidationException("data de nascimento não pode ser maior que hoje");
    }

    public static BirthDate of(LocalDate value, Clock clock) {
        return new BirthDate(value, LocalDate.now(clock));
    }

    @Override
    protected LocalDate validate(LocalDate value) {
        return value;
    }
}
