package com.crm.common.infrastructure.seed;

import com.crm.common.application.exceptions.InternalServerErrorException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.PlaceholderResolutionException;

import java.io.IOException;
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

            List<?> data = load(seeder);

            transactionTemplate.execute(status -> {
                seeder.seed((List) data);
                return null;
            });
        }

        log.info("seeding do banco de dados concluído");
    }

    /**
     * Lê o JSON ainda com os {@code ${...}}, que não tem segredo, e só então troca o valor de cada
     * texto. Trocar no texto do JSON antes de parsear quebraria com aspas e barra no valor (ou o
     * alteraria em silêncio) e levaria o valor para a mensagem do erro de parse.
     *
     * <p>Falha de leitura ou de formato vira exception do sistema sem a causa: a mensagem da causa
     * pode repetir o valor da variável.
     */
    private List<?> load(Seeder<?> seeder) throws IOException {
        var path = seeder.resourcePath();
        var raw = new String(new ClassPathResource(path).getInputStream().readAllBytes(), StandardCharsets.UTF_8);

        try {
            var tree = resolveTexts(objectMapper.readTree(raw));

            var type = objectMapper.getTypeFactory()
                    .constructCollectionType(List.class, seeder.recordType());

            return objectMapper.readerFor(type).readValue(tree);
        }
        catch (JsonProcessingException e) {
            throw new InternalServerErrorException("seed: JSON inválido em " + path);
        }
    }

    /** Troca os {@code ${...}} de cada valor de texto do JSON; chaves e outros tipos ficam como estão. */
    private JsonNode resolveTexts(JsonNode node) {
        if (node.isTextual())
            return TextNode.valueOf(resolve(node.asText()));

        if (node instanceof ObjectNode object)
            for (var field : object.properties())
                field.setValue(resolveTexts(field.getValue()));

        if (node instanceof ArrayNode array)
            for (var i = 0; i < array.size(); i++)
                array.set(i, resolveTexts(array.get(i)));

        return node;
    }

    /**
     * Resolve {@code ${...}} contra o Environment, igual a um application.properties.
     * Placeholder sem valor derruba o boot; a mensagem leva só o nome da variável, porque o
     * texto original do Spring traz o texto inteiro (que pode ter senha).
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
