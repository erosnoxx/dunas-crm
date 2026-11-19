package com.crm.common.infrastructure.seed;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;
import java.nio.charset.StandardCharsets;
import java.util.List;

/** Bean registrado em {@code SeedConfig} (architecture.md §9, sem component-scan). */
@Slf4j @RequiredArgsConstructor
@SuppressWarnings("unchecked")
public class DatabaseSeeder implements ApplicationRunner {
    private final List<Seeder<?>> seeders;
    private final ObjectMapper objectMapper;
    private final Environment environment;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        log.info("Iniciando o seeding do banco de dados...");

        for (var seeder : seeders) {
            log.info("seeding: {}", seeder.getClass().getSimpleName());

            var resource = new ClassPathResource(seeder.resourcePath());
            var raw = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            // ${...} no JSON do seeder é resolvido contra o Environment, igual a um application.properties
            var resolved = environment.resolvePlaceholders(raw);

            var type = objectMapper.getTypeFactory()
                    .constructCollectionType(List.class, seeder.recordType());
            List<?> data = objectMapper.readValue(resolved, type);
            seeder.seed((List) data);
        }

        log.info("Seeding do banco de dados concluído.");
    }
}
