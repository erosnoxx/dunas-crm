package com.crm.common.infrastructure.storage;

import com.crm.common.application.storage.FileStorage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("StorageConfig")
class StorageConfigTest {

    @Configuration
    static class ClockConfig {

        @Bean
        Clock clock() {
            return Clock.systemUTC();
        }
    }

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(ClockConfig.class, StorageConfig.class);

    @Test
    @DisplayName("sem crm.storage.endpoint, nenhum FileStorage é criado e o app sobe")
    void noEndpointNoBean() {
        runner.run(context -> assertThat(context).doesNotHaveBean(FileStorage.class));
    }

    @Test
    @DisplayName("com o endpoint, cria o FileStorage e assina a URL no host público")
    void withEndpointCreatesTheBean() {
        runner
                .withPropertyValues(
                        "crm.storage.endpoint=http://seaweedfs:8333",
                        "crm.storage.public-endpoint=http://localhost:8333",
                        "crm.storage.access-key=dev-key",
                        "crm.storage.secret-key=dev-secret"
                )
                .run(context -> {
                    assertThat(context).hasSingleBean(FileStorage.class);

                    var storage = context.getBean(FileStorage.class);

                    assertThat(storage.publicUrl("events/a/cover.png"))
                            .isEqualTo("http://localhost:8333/crm-public/events/a/cover.png");
                    assertThat(storage.prepareUpload("events/a/cover.png", "image/png", Duration.ofMinutes(5)).url())
                            .startsWith("http://localhost:8333/crm-public/events/a/cover.png?");
                });
    }
}
