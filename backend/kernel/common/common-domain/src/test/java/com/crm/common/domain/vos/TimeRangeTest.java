package com.crm.common.domain.vos;

import com.crm.common.domain.exceptions.ValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("TimeRange")
class TimeRangeTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    @Test
    @DisplayName("aceita início antes do fim")
    void acceptsValidRange() {
        var range = TimeRange.of(NOW, NOW.plus(1, ChronoUnit.HOURS));

        assertEquals(NOW, range.start());
        assertEquals(NOW.plus(1, ChronoUnit.HOURS), range.end());
    }

    @Test
    @DisplayName("recusa início ou fim nulos")
    void rejectsNulls() {
        assertEquals(
                "período não pode ter início ou fim nulos",
                assertThrows(ValidationException.class, () -> TimeRange.of(null, NOW)).getMessage()
        );

        assertThrows(ValidationException.class, () -> TimeRange.of(NOW, null));
    }

    @Test
    @DisplayName("recusa fim igual ou anterior ao início")
    void rejectsEndNotAfterStart() {
        assertEquals(
                "fim do período deve ser depois do início",
                assertThrows(ValidationException.class, () -> TimeRange.of(NOW, NOW)).getMessage()
        );

        assertThrows(ValidationException.class, () -> TimeRange.of(NOW, NOW.minusSeconds(1)));
    }

    @Test
    @DisplayName("contains inclui as pontas e exclui fora do intervalo")
    void containsIsInclusive() {
        var range = TimeRange.of(NOW, NOW.plus(1, ChronoUnit.HOURS));

        assertTrue(range.contains(NOW));
        assertTrue(range.contains(NOW.plus(1, ChronoUnit.HOURS)));
        assertTrue(range.contains(NOW.plus(30, ChronoUnit.MINUTES)));

        assertFalse(range.contains(NOW.minusSeconds(1)));
        assertFalse(range.contains(NOW.plus(1, ChronoUnit.HOURS).plusSeconds(1)));
    }

    @Test
    @DisplayName("contains recusa instante nulo")
    void containsRejectsNull() {
        var range = TimeRange.of(NOW, NOW.plus(1, ChronoUnit.HOURS));

        assertThrows(ValidationException.class, () -> range.contains(null));
    }

    @Test
    @DisplayName("igualdade é por valor — record")
    void equalityByValue() {
        assertEquals(
                TimeRange.of(NOW, NOW.plus(1, ChronoUnit.HOURS)),
                TimeRange.of(NOW, NOW.plus(1, ChronoUnit.HOURS))
        );
    }
}
