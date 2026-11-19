package com.crm.common.domain.vos;

import java.time.LocalDate;

/**
 * Base dos Value Objects que embrulham uma data sem horário.
 */
public abstract class LocalDateValueObject extends ValueObject<LocalDate> {
    protected LocalDateValueObject(LocalDate value) {
        super(value);
    }
}
