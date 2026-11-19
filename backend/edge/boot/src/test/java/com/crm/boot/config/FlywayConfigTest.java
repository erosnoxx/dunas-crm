package com.crm.boot.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("FlywayConfig")
class FlywayConfigTest {

    @Test
    @DisplayName("descobre do classpath os módulos que têm migrations")
    void discoversModulesFromClasspath() {
        assertThat(FlywayConfig.discoverModules()).contains("common");
    }

    @Test
    @DisplayName("nomeia o bean de cada módulo como <módulo>Flyway")
    void namesBeansAfterTheModule() {
        assertThat(FlywayConfig.beanName("common")).isEqualTo("commonFlyway");
    }
}
