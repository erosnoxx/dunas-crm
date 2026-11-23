package com.crm.registry.domain.customers.vos;

import com.crm.common.domain.exceptions.DomainException;
import com.crm.common.domain.exceptions.ValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("InstallationDate")
class InstallationDateTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-22T12:00:00Z"), ZoneOffset.UTC);

    @Test
    @DisplayName("aceita hoje")
    void acceptsToday() {
        assertThat(InstallationDate.of(LocalDate.of(2026, 11, 22), CLOCK).getValue())
                .isEqualTo(LocalDate.of(2026, 11, 22));
    }

    @Test
    @DisplayName("aceita ontem")
    void acceptsYesterday() {
        assertThat(InstallationDate.of(LocalDate.of(2026, 11, 21), CLOCK).getValue())
                .isEqualTo(LocalDate.of(2026, 11, 21));
    }

    @Test
    @DisplayName("recusa amanhã com o código invalid_installation_date")
    void rejectsTomorrow() {
        assertThatThrownBy(() -> InstallationDate.of(LocalDate.of(2026, 11, 23), CLOCK))
                .isInstanceOf(ValidationException.class)
                .hasMessage("a data de instalação não pode ser depois de hoje")
                .satisfies(e -> assertThat(((DomainException) e).getCode()).contains("invalid_installation_date"));
    }

    @Test
    @DisplayName("recusa nulo")
    void rejectsNull() {
        assertThatThrownBy(() -> InstallationDate.of(null, CLOCK)).isInstanceOf(ValidationException.class);
    }
}
