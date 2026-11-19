package com.crm.common.infrastructure.seed;

import com.crm.common.application.exceptions.InternalServerErrorException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.PlaceholderResolutionException;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Roda os {@link Seeder} no boot. Bean registrado em {@code SeedConfig} (architecture.md §9,
 * sem component-scan).
 *
 * <p>É seguro para produção: pula o seeder já semeado, grava cada um numa transação e faz o
 * boot falhar (sem semear o resto) se um placeholder do JSON não tiver valor ou se o seed lançar.
 */
@Slf4j @RequiredArgsConstructor
@SuppressWarnings("unchecked")
public class DatabaseSeeder implements ApplicationRunner {
    private final List<Seeder<?>> seeders;
    private final ObjectMapper objectMapper;
    private final Environment environment;
    private final TransactionTemplate transactionTemplate;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        log.info("iniciando o seeding do banco de dados");

        for (var seeder : seeders) {
            var name = seeder.getClass().getSimpleName();

            if (seeder.isAlreadySeeded()) {
                log.info("seeding já feito, pulando: {}", name);
                continue;
            }

            log.info("seeding: {}", name);

            var resource = new ClassPathResource(seeder.resourcePath());
            var raw = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            var resolved = resolve(raw);

            var type = objectMapper.getTypeFactory()
                    .constructCollectionType(List.class, seeder.recordType());
            List<?> data = objectMapper.readValue(resolved, type);

            transactionTemplate.execute(status -> {
                seeder.seed((List) data);
                return null;
            });
        }

        log.info("seeding do banco de dados concluído");
    }

    /**
     * Resolve {@code ${...}} do JSON contra o Environment, igual a um application.properties.
     * Placeholder sem valor derruba o boot; a mensagem leva só o nome da variável, porque o
     * texto original do Spring traz o JSON inteiro (que pode ter senha).
     */
    private String resolve(String raw) {
        try {
            return environment.resolveRequiredPlaceholders(raw);
        }
        catch (PlaceholderResolutionException e) {
            throw new InternalServerErrorException("seed: variável não definida: " + e.getPlaceholder());
        }
    }
}
