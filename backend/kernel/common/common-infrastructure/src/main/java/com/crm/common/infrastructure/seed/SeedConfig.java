package com.crm.common.infrastructure.seed;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.util.List;

/** Liga o seeding. Cada BC registra os próprios {@link Seeder} na própria configuração. */
@Configuration
public class SeedConfig {

    @Bean
    public DatabaseSeeder databaseSeeder(
            List<Seeder<?>> seeders,
            ObjectMapper objectMapper,
            Environment environment
    ) {
        return new DatabaseSeeder(seeders, objectMapper, environment);
    }
}
