package com.crm.iam.infrastructure;

import com.crm.common.application.ratelimit.RateLimit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("IamProperties")
class IamPropertiesTest {

    private static final String LIMITED = "muitas tentativas; tente de novo em alguns minutos";

    private static IamProperties bind(Map<String, String> source) {
        return new Binder(new MapConfigurationPropertySource(source))
                .bindOrCreate("crm.iam", IamProperties.class);
    }

    @Test
    @DisplayName("sem nenhuma configuração, os defaults são os valores da spec")
    void defaultsMatchTheSpec() {
        var properties = bind(Map.of());

        assertThat(properties.sessionValidity()).isEqualTo(Duration.ofDays(30));
        assertThat(properties.sessionRenewInterval()).isEqualTo(Duration.ofMinutes(5));
        assertThat(properties.sessionCacheTtl()).isEqualTo(Duration.ofSeconds(60));
        assertThat(properties.invitationValidity()).isEqualTo(Duration.ofHours(48));
        assertThat(properties.passwordResetValidity()).isEqualTo(Duration.ofHours(1));

        assertThat(properties.loginLimitPerEmail().maxAttempts()).isEqualTo(5);
        assertThat(properties.loginLimitPerEmail().window()).isEqualTo(Duration.ofMinutes(15));
        assertThat(properties.loginLimitPerOrigin().maxAttempts()).isEqualTo(20);
        assertThat(properties.loginLimitPerOrigin().window()).isEqualTo(Duration.ofMinutes(15));
        assertThat(properties.passwordResetLimitPerEmail().maxAttempts()).isEqualTo(3);
        assertThat(properties.passwordResetLimitPerEmail().window()).isEqualTo(Duration.ofHours(1));
    }

    @Test
    @DisplayName("os limites viram RateLimit com o escopo do IAM e recusam quando o contador cai")
    void settingsBuildsFailClosedLimitsWithIamScopes() {
        var settings = bind(Map.of()).toSettings();

        assertThat(settings.loginPerEmail()).isEqualTo(
                RateLimit.failClosed("iam.login.email", 5, Duration.ofMinutes(15), "rate_limited", LIMITED));
        assertThat(settings.loginPerOrigin()).isEqualTo(
                RateLimit.failClosed("iam.login.origin", 20, Duration.ofMinutes(15), "rate_limited", LIMITED));
        assertThat(settings.passwordResetPerEmail()).isEqualTo(
                RateLimit.failClosed("iam.password-reset.email", 3, Duration.ofHours(1), "rate_limited", LIMITED));
        assertThat(settings.sessionValidity()).isEqualTo(Duration.ofDays(30));
        assertThat(settings.invitationValidity()).isEqualTo(Duration.ofHours(48));
    }

    @Test
    @DisplayName("o Argon2 tem defaults e o appUrl aponta para o frontend local")
    void argon2AndAppUrlHaveDefaults() {
        var properties = bind(Map.of());

        assertThat(properties.appUrl()).isEqualTo("http://localhost:5173");
        assertThat(properties.argon2().memoryKib()).isEqualTo(19456);
        assertThat(properties.argon2().iterations()).isEqualTo(2);
        assertThat(properties.argon2().parallelism()).isEqualTo(1);
        assertThat(properties.argon2().saltLength()).isEqualTo(16);
        assertThat(properties.argon2().hashLength()).isEqualTo(32);
    }

    @Test
    @DisplayName("configurar só um campo do limite mantém o outro no default")
    void overridingOneLimitFieldKeepsTheOtherDefault() {
        var properties = bind(Map.of("crm.iam.login-limit-per-email.max-attempts", "10"));

        assertThat(properties.loginLimitPerEmail().maxAttempts()).isEqualTo(10);
        assertThat(properties.loginLimitPerEmail().window()).isEqualTo(Duration.ofMinutes(15));
        assertThat(properties.loginLimitPerOrigin().maxAttempts()).isEqualTo(20);
    }

    @Test
    @DisplayName("valores configurados sobrescrevem os defaults")
    void configuredValuesOverrideDefaults() {
        var properties = bind(Map.of(
                "crm.iam.session-validity", "7d",
                "crm.iam.app-url", "https://crm.dunas.com.br",
                "crm.iam.argon2.memory-kib", "65536"
        ));

        assertThat(properties.sessionValidity()).isEqualTo(Duration.ofDays(7));
        assertThat(properties.appUrl()).isEqualTo("https://crm.dunas.com.br");
        assertThat(properties.argon2().memoryKib()).isEqualTo(65536);
        assertThat(properties.argon2().iterations()).isEqualTo(2);
    }
}
