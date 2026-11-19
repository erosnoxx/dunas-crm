package com.crm.shared.domain.identity;

import com.crm.common.domain.exceptions.ValidationException;
import com.crm.shared.domain.vos.identity.BirthDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("BirthDate")
class BirthDateTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-24T12:00:00Z"), ZoneOffset.UTC);

    @Test
    @DisplayName("aceita data no passado")
    void acceptsPastDate() {
        assertEquals(LocalDate.of(1990, 5, 17), BirthDate.of(LocalDate.of(1990, 5, 17), CLOCK).getValue());
    }

    @Test
    @DisplayName("aceita hoje")
    void acceptsToday() {
        assertEquals(LocalDate.of(2026, 10, 24), BirthDate.of(LocalDate.of(2026, 10, 24), CLOCK).getValue());
    }

    @Test
    @DisplayName("recusa data no futuro")
    void rejectsFutureDate() {
        var exception = assertThrows(
                ValidationException.class,
                () -> BirthDate.of(LocalDate.of(2026, 10, 25), CLOCK)
        );

        assertEquals("data de nascimento não pode ser maior que hoje", exception.getMessage());
    }

    @Test
    @DisplayName("recusa nulo")
    void rejectsNull() {
        assertThrows(ValidationException.class, () -> BirthDate.of(null, CLOCK));
    }

    @Test
    @DisplayName("datas iguais são o mesmo valor")
    void equalsByValue() {
        assertEquals(BirthDate.of(LocalDate.of(1990, 5, 17), CLOCK), BirthDate.of(LocalDate.of(1990, 5, 17), CLOCK));
    }
}
