package com.crm.iam.application;

import com.crm.common.application.exceptions.InternalServerErrorException;
import com.crm.common.application.ratelimit.RateLimit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("IamSettings")
class IamSettingsTest {

    private static final RateLimit LIMIT = RateLimit.failOpen("iam:test", 5, Duration.ofMinutes(15));

    private static IamSettings settings(Duration[] durations) {
        return new IamSettings(
                durations[0],
                durations[1],
                durations[2],
                durations[3],
                durations[4],
                LIMIT,
                LIMIT,
                LIMIT
        );
    }

    private static Duration[] valid() {
        return new Duration[]{
                Duration.ofDays(30),
                Duration.ofMinutes(5),
                Duration.ofSeconds(60),
                Duration.ofHours(48),
                Duration.ofHours(1)
        };
    }

    @Test
    @DisplayName("aceita durações positivas e limites informados")
    void acceptsValidValues() {
        var settings = settings(valid());

        assertThat(settings.sessionValidity()).isEqualTo(Duration.ofDays(30));
        assertThat(settings.passwordResetValidity()).isEqualTo(Duration.ofHours(1));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 3, 4})
    @DisplayName("recusa duração nula, zero ou negativa em qualquer posição")
    void refusesNullZeroAndNegativeDurations(int index) {
        for (var invalid : new Duration[]{null, Duration.ZERO, Duration.ofSeconds(-1)}) {
            var durations = valid();
            durations[index] = invalid;

            assertThatThrownBy(() -> settings(durations))
                    .isInstanceOf(InternalServerErrorException.class);
        }
    }

    @Test
    @DisplayName("recusa limite de tentativas nulo")
    void refusesNullRateLimit() {
        var durations = valid();

        assertThatThrownBy(() -> new IamSettings(
                durations[0],
                durations[1],
                durations[2],
                durations[3],
                durations[4],
                LIMIT,
                null,
                LIMIT
        )).isInstanceOf(InternalServerErrorException.class);
    }
}
