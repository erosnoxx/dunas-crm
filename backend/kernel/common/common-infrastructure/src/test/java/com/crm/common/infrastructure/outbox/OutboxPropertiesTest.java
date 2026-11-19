package com.crm.common.infrastructure.outbox;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Os comentários dos {@code application*.properties} dizem que estes ajustes têm default no
 * código e por isso não precisam ser repetidos lá. Este teste é o que sustenta essa afirmação:
 * binding por construtor em record com {@code @DefaultValue} falha em silêncio se estiver
 * mal configurado — os valores viriam zerados ou nulos, não com o default.
 */
@DisplayName("OutboxProperties")
class OutboxPropertiesTest {

    @Configuration
    @EnableConfigurationProperties(OutboxProperties.class)
    static class TestConfig {
    }

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(org.springframework.boot.autoconfigure.AutoConfigurations.of(
                    ConfigurationPropertiesAutoConfiguration.class))
            .withUserConfiguration(TestConfig.class);

    @Test
    @DisplayName("aplica os defaults quando nada é configurado")
    void appliesDefaults() {
        runner.run(context -> {
            var properties = context.getBean(OutboxProperties.class);

            assertThat(properties.batchSize()).isEqualTo(100);
            assertThat(properties.maxAttempts()).isEqualTo(5);
            assertThat(properties.retryBackoff()).isEqualTo(Duration.ofSeconds(10));
            assertThat(properties.pollDelay()).isEqualTo(Duration.ofSeconds(5));
        });
    }

    /**
     * Os perfis usam a forma "2s". O valor cru em milissegundos também precisa continuar
     * funcionando: era o formato de quando {@code poll-delay} morava inline no {@code @Scheduled},
     * e alguém pode ter esse valor num ambiente já configurado.
     */
    @Test
    @DisplayName("aceita duração por sufixo e número cru em milissegundos")
    void parsesBothDurationFormats() {
        runner.withPropertyValues("crm.outbox.poll-delay=2s")
                .run(context -> assertThat(context.getBean(OutboxProperties.class).pollDelay())
                        .isEqualTo(Duration.ofSeconds(2)));

        runner.withPropertyValues("crm.outbox.poll-delay=2000")
                .run(context -> assertThat(context.getBean(OutboxProperties.class).pollDelay())
                        .isEqualTo(Duration.ofMillis(2000)));
    }

    @Test
    @DisplayName("propriedade declarada sobrescreve o default, as demais permanecem")
    void overridesOnlyWhatIsDeclared() {
        runner
                .withPropertyValues(
                        "crm.outbox.batch-size=200",
                        "crm.outbox.retry-backoff=30s"
                )
                .run(context -> {
                    var properties = context.getBean(OutboxProperties.class);

                    assertThat(properties.batchSize()).isEqualTo(200);
                    assertThat(properties.retryBackoff()).isEqualTo(Duration.ofSeconds(30));

                    // não declarada no perfil: continua no default
                    assertThat(properties.maxAttempts()).isEqualTo(5);
                });
    }
}
