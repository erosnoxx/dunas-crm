package com.crm.registry.domain.customers.vos;

import com.crm.common.domain.exceptions.ValidationException;
import com.crm.common.domain.vos.LocalDateValueObject;

import java.time.Clock;
import java.time.LocalDate;

/**
 * Data de instalação de uma usina ou equipamento: hoje é válido, o futuro não.
 *
 * <p>"Hoje" vem do {@link Clock} recebido, nunca do relógio do sistema.
 */
public final class InstallationDate extends LocalDateValueObject {

    private InstallationDate(LocalDate value, LocalDate today) {
        super(value);

        if (value.isAfter(today))
            throw new ValidationException("invalid_installation_date", "a data de instalação não pode ser depois de hoje");
    }

    public static InstallationDate of(LocalDate value, Clock clock) {
        return new InstallationDate(value, LocalDate.now(clock));
    }

    @Override
    protected LocalDate validate(LocalDate value) {
        return value;
    }
}
