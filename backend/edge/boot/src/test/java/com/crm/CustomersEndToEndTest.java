package com.crm;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;

import java.net.http.HttpClient;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Os clientes inteiros no contexto real da aplicação: Tomcat numa porta aleatória, Postgres e Redis
 * do Testcontainers (exige Docker), a sessão e os papéis do IAM de verdade e o mesmo cliente HTTP
 * com cookies do {@link RegistryEndToEndTest}.
 *
 * <p>Cada cenário usa documentos só seus, então a ordem dos cenários não importa; o primeiro só
 * garante que o Administrador troca a senha inicial.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "CRM_ADMIN_NAME=Administradora Inicial",
                "CRM_ADMIN_EMAIL=" + EndToEndAccounts.ADMIN_EMAIL,
                "CRM_ADMIN_PASSWORD=" + EndToEndAccounts.ADMIN_INITIAL_PASSWORD,
                "spring.mail.host=localhost",
                "crm.email.from=nao-responda@dunas.test",
                "management.health.rabbit.enabled=false",
                "management.health.mail.enabled=false",
                "crm.outbox.poll-delay=1h"
        }
)
@Import(EndToEndTestBeans.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Clientes ponta a ponta no servidor real")
class CustomersEndToEndTest {

    private static final String SERIAL = "HV2240187563";

    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16");

    @ServiceConnection(name = "redis")
    static final GenericContainer<?> REDIS = new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

    static {
        POSTGRES.start();
        REDIS.start();
    }

    @LocalServerPort
    private int port;

    @Autowired
    private CapturingEmailSender emails;

    private final HttpClient http = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build();
    private final Set<String> secrets = ConcurrentHashMap.newKeySet();

    @Test
    @Order(1)
    @DisplayName("fluxo completo: cadastra, acha pela série, desativa, e o Técnico só consulta")
    void fullFlow() {
        var admin = accounts().admin();
        var modelId = createModel(admin);

        var customerId = created(admin.post("/customers", Map.of("document", "529.982.247-25", "name", "Maria Silva", "email", "maria@dunas.test")));
        var repeated = admin.post("/customers", Map.of("document", "52998224725", "name", "Maria Repetida", "email", "outra@dunas.test"));

        assertThat(repeated.status()).isEqualTo(409);
        assertThat(repeated.code()).isEqualTo("customer_document_taken");
        assertThat(repeated.raw()).doesNotContain("52998224725").doesNotContain("529.982.247-25");

        var updated = admin.put(
                "/customers/" + customerId,
                Map.of("document", "111.444.777-35", "name", "Maria Souza", "email", "maria@dunas.test")
        );

        assertThat(updated.status()).isEqualTo(204);

        var afterUpdate = admin.get("/customers/" + customerId).body();

        assertThat(afterUpdate.get("document").asText()).isEqualTo("52998224725");
        assertThat(afterUpdate.get("name").asText()).isEqualTo("Maria Souza");

        var locationId = created(admin.post("/customers/" + customerId + "/locations", locationBody("Casa")));

        assertThat(admin.get("/customers/" + customerId).body().get("locations").get(0).get("address").get("zipCode").asText())
                .isEqualTo("01310100");

        created(admin.post("/locations/" + locationId + "/equipment", Map.of("modelId", modelId, "serialNumber", SERIAL)));

        var sameSerial = admin.post(
                "/locations/" + locationId + "/equipment",
                Map.of("modelId", modelId, "serialNumber", SERIAL.toLowerCase())
        );

        assertThat(sameSerial.status()).isEqualTo(409);
        assertThat(sameSerial.code()).isEqualTo("serial_number_taken");

        var found = admin.get("/customers?text=hv2240");

        assertThat(found.status()).isEqualTo(200);
        assertThat(found.body().get("content")).hasSize(1);
        assertThat(found.body().get("content").get(0).get("id").asText()).isEqualTo(customerId.toString());
        assertThat(found.body().get("content").get(0).get("matchedSerials").get(0).get("serialNumber").asText()).isEqualTo(SERIAL);

        assertThat(admin.post("/customers/" + customerId + "/deactivation", Map.of()).status()).isEqualTo(204);
        assertThat(admin.get("/customers?text=hv2240").body().get("content")).isEmpty();

        var all = admin.get("/customers?text=hv2240&status=ALL").body().get("content");

        assertThat(all).hasSize(1);
        assertThat(all.get(0).get("status").asText()).isEqualTo("DEACTIVATED");

        assertThat(admin.put("/locations/" + locationId, locationBody("Casa de praia")).status()).isEqualTo(204);

        var newLocation = admin.post("/customers/" + customerId + "/locations", locationBody("Galpão"));

        assertThat(newLocation.status()).isEqualTo(422);
        assertThat(newLocation.code()).isEqualTo("customer_deactivated");

        var technician = accounts().technician(admin, "tecnico.clientes@dunas.test");

        assertThat(technician.get("/customers?status=ALL").status()).isEqualTo(200);
        assertThat(technician.get("/customers/" + customerId).status()).isEqualTo(200);

        var refused = technician.post("/customers", Map.of("document", "11.222.333/0001-81", "name", "Solar do Tecnico", "phone", "+5511987654321"));

        assertThat(refused.status()).isEqualTo(403);
        assertThat(refused.code()).isEqualTo("forbidden");
    }

    @Test
    @Order(2)
    @DisplayName("sem contato é 422 contact_required e CPF com nome fantasia é 422 trade_name_not_allowed")
    void aggregateRules() {
        var admin = accounts().admin();

        var withoutContact = admin.post("/customers", Map.of("document", "111.444.777-35", "name", "Joao Sem Contato"));

        assertThat(withoutContact.status()).isEqualTo(422);
        assertThat(withoutContact.code()).isEqualTo("contact_required");

        var tradeName = admin.post(
                "/customers",
                Map.of("document", "111.444.777-35", "name", "Joao Fantasia", "tradeName", "Joao Solar", "email", "joao@dunas.test")
        );

        assertThat(tradeName.status()).isEqualTo(422);
        assertThat(tradeName.code()).isEqualTo("trade_name_not_allowed");
    }

    @Test
    @Order(3)
    @DisplayName("sem sessão é 401 e status desconhecido na lista é 400")
    void sessionAndStatus() {
        var anonymous = accounts().browser("anonimo").get("/customers");

        assertThat(anonymous.status()).isEqualTo(401);
        assertThat(anonymous.code()).isEqualTo("session_invalid");
        assertThat(accounts().admin().get("/customers?status=FOO").status()).isEqualTo(400);
    }

    @Test
    @Order(4)
    @DisplayName("cadastro simultâneo do mesmo documento é um 201 e um 409 customer_document_taken, nunca 500")
    void concurrentRegistration() throws Exception {
        var admin = accounts().admin();
        var start = new CountDownLatch(1);
        var pool = Executors.newFixedThreadPool(2);

        try {
            var tasks = List.of(1, 2).stream()
                    .map(attempt -> pool.submit(() -> {
                        start.await();

                        return admin.post(
                                "/customers",
                                Map.of("document", "11.222.333/0001-81", "name", "Solar Ltda " + attempt, "phone", "+5511987654321")
                        );
                    }))
                    .toList();

            start.countDown();

            var replies = List.of(tasks.get(0).get(30, TimeUnit.SECONDS), tasks.get(1).get(30, TimeUnit.SECONDS));

            assertThat(replies).extracting(Reply::status).containsExactlyInAnyOrder(201, 409);
            assertThat(replies).filteredOn(reply -> reply.status() == 409)
                    .extracting(Reply::code)
                    .containsExactly("customer_document_taken");
        }
        finally {
            pool.shutdownNow();
        }
    }

    // --- apoio ---------------------------------------------------------------------------------

    /** Fabricante, tipo e modelo criados pelo catálogo; devolve o id do modelo. */
    private UUID createModel(Browser admin) {
        var manufacturerId = created(admin.post("/catalog/manufacturers", Map.of("name", "Huawei Clientes")));
        var typeId = created(admin.post(
                "/catalog/equipment-types",
                Map.of("name", "Inversor Clientes", "characteristic", Map.of("name", "Potência", "unit", "KILOWATT", "required", true))
        ));
        var type = admin.get("/catalog/equipment-types?size=100").body().get("content");
        var definitionId = "";

        for (var candidate : type)
            if (typeId.toString().equals(candidate.get("id").asText()))
                definitionId = candidate.get("characteristics").get(0).get("id").asText();

        return created(admin.post(
                "/catalog/equipment-models",
                Map.of(
                        "manufacturerId", manufacturerId,
                        "typeId", typeId,
                        "name", "SUN2000-5KTL-L1",
                        "values", List.of(Map.of("definitionId", definitionId, "amount", "5"))
                )
        ));
    }

    private static Map<String, Object> locationBody(String name) {
        return Map.of(
                "name", name,
                "address", Map.of(
                        "zipCode", "01310-100",
                        "street", "Avenida Paulista",
                        "number", "1000",
                        "neighborhood", "Bela Vista",
                        "city", "São Paulo",
                        "state", "SP"
                )
        );
    }

    private static UUID created(Reply reply) {
        assertThat(reply.status()).as(reply.raw()).isEqualTo(201);

        return UUID.fromString(reply.body().get("id").asText());
    }

    private EndToEndAccounts accounts() {
        return new EndToEndAccounts(http, port, secrets, emails);
    }
}
