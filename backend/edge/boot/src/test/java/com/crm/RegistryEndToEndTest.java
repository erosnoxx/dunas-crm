package com.crm;

import com.fasterxml.jackson.databind.JsonNode;
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

import java.math.BigDecimal;
import java.net.http.HttpClient;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * O catálogo inteiro no contexto real da aplicação: Tomcat numa porta aleatória, Postgres e Redis do
 * Testcontainers (exige Docker), a sessão e os papéis do IAM de verdade e o mesmo cliente HTTP com
 * cookies do {@link IamEndToEndTest}.
 *
 * <p>O catálogo nasce vazio (o seed é {@code []}), e cada cenário usa nomes só seus, então a ordem
 * dos cenários não importa; o primeiro só garante que o Administrador troca a senha inicial.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "CRM_ADMIN_NAME=Administradora Inicial",
                "CRM_ADMIN_EMAIL=" + RegistryEndToEndTest.ADMIN_EMAIL,
                "CRM_ADMIN_PASSWORD=" + RegistryEndToEndTest.ADMIN_INITIAL_PASSWORD,
                "spring.mail.host=localhost",
                "crm.email.from=nao-responda@dunas.test",
                "management.health.rabbit.enabled=false",
                "management.health.mail.enabled=false",
                "crm.outbox.poll-delay=1h"
        }
)
@Import(EndToEndTestBeans.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Catálogo ponta a ponta no servidor real")
class RegistryEndToEndTest {

    static final String ADMIN_EMAIL = "admin@dunas.test";
    static final String ADMIN_INITIAL_PASSWORD = "Inicial#2026";
    static final String ADMIN_PASSWORD = "Trocada#2026";
    static final String TECHNICIAN_PASSWORD = "Tecnico#2026";

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
    private final Set<String> secrets = new HashSet<>();

    @Test
    @Order(1)
    @DisplayName("fluxo completo: cadastra, busca, desativa e reativa, e o Técnico só consulta")
    void fullFlow() {
        var admin = adminBrowser();

        var anonymous = browser("anonimo").get("/catalog/equipment-models");

        assertThat(anonymous.status()).isEqualTo(401);
        assertThat(anonymous.code()).isEqualTo("session_invalid");

        var manufacturerId = createManufacturer(admin, "Huawei Fluxo");
        var repeated = admin.post("/catalog/manufacturers", Map.of("name", "HUAWEI FLUXO"));

        assertThat(repeated.status()).isEqualTo(409);
        assertThat(repeated.code()).isEqualTo("manufacturer_name_taken");

        var typeId = createType(admin, "Inversor", "Potência", "KILOWATT");
        var definitionId = definitionIdOf(admin, typeId);
        var modelId = createModel(admin, manufacturerId, typeId, definitionId, "SUN2000-3KTL-L1", "3");

        var found = admin.get("/catalog/equipment-models?text=sun2000");

        assertThat(found.status()).isEqualTo(200);
        assertThat(found.body().get("content")).hasSize(1);

        var model = found.body().get("content").get(0);

        assertThat(model.get("id").asText()).isEqualTo(modelId.toString());
        assertThat(model.get("name").asText()).isEqualTo("SUN2000-3KTL-L1");
        assertThat(model.get("status").asText()).isEqualTo("ACTIVE");
        assertThat(model.get("manufacturer").get("id").asText()).isEqualTo(manufacturerId.toString());
        assertThat(model.get("manufacturer").get("name").asText()).isEqualTo("Huawei Fluxo");
        assertThat(model.get("type").get("id").asText()).isEqualTo(typeId.toString());
        assertThat(model.get("type").get("name").asText()).isEqualTo("Inversor");
        assertThat(model.get("values")).hasSize(1);
        assertThat(model.get("values").get(0).get("definitionId").asText()).isEqualTo(definitionId.toString());
        assertThat(model.get("values").get(0).get("name").asText()).isEqualTo("Potência");
        assertThat(model.get("values").get(0).get("unit").asText()).isEqualTo("KILOWATT");
        assertThat(model.get("values").get(0).get("amount").decimalValue()).isEqualByComparingTo(BigDecimal.valueOf(3));
        assertThat(admin.get("/catalog/equipment-models/" + modelId).status()).isEqualTo(200);

        assertThat(admin.post("/catalog/equipment-models/" + modelId + "/deactivation", Map.of()).status()).isEqualTo(204);
        assertThat(admin.get("/catalog/equipment-models?text=sun2000").body().get("content")).isEmpty();

        var all = admin.get("/catalog/equipment-models?text=sun2000&status=ALL").body().get("content");

        assertThat(all).hasSize(1);
        assertThat(all.get(0).get("status").asText()).isEqualTo("DEACTIVATED");

        assertThat(admin.post("/catalog/equipment-models/" + modelId + "/reactivation", Map.of()).status()).isEqualTo(204);
        assertThat(admin.get("/catalog/equipment-models?text=sun2000").body().get("content")).hasSize(1);

        var technician = technicianBrowser(admin, "tecnico.catalogo@dunas.test");

        assertThat(technician.get("/catalog/equipment-models?text=sun2000").status()).isEqualTo(200);
        assertThat(technician.get("/catalog/equipment-types").status()).isEqualTo(200);
        assertThat(technician.get("/catalog/manufacturers").status()).isEqualTo(200);

        var refused = technician.post(
                "/catalog/equipment-models",
                Map.of(
                        "manufacturerId", manufacturerId,
                        "typeId", typeId,
                        "name", "SUN2000-5KTL-L1",
                        "values", List.of(Map.of("definitionId", definitionId, "amount", "5"))
                )
        );

        assertThat(refused.status()).isEqualTo(403);
        assertThat(refused.code()).isEqualTo("forbidden");
        assertThat(technician.post("/catalog/manufacturers", Map.of("name", "Marca do Tecnico")).code())
                .isEqualTo("forbidden");
    }

    @Test
    @Order(2)
    @DisplayName("tipo com modelo: trocar a unidade é 422 characteristic_locked e o tipo não muda")
    void lockedCharacteristic() {
        var admin = adminBrowser();
        var manufacturerId = createManufacturer(admin, "Solis Trava");
        var typeId = createType(admin, "Inversor Trava", "Potência", "KILOWATT");

        createModel(admin, manufacturerId, typeId, definitionIdOf(admin, typeId), "S6-GR1P3K", "3");

        var changed = admin.put("/catalog/equipment-types/" + typeId, typeBody("Inversor Trava", "Potência", "WATT"));

        assertThat(changed.status()).isEqualTo(422);
        assertThat(changed.code()).isEqualTo("characteristic_locked");

        var type = typeOf(admin, typeId);

        assertThat(type.get("characteristics").get(0).get("unit").asText()).isEqualTo("KILOWATT");
        assertThat(type.get("characteristics").get(0).get("name").asText()).isEqualTo("Potência");
    }

    @Test
    @Order(3)
    @DisplayName("renomear a característica do tipo duas vezes seguidas é 204 nas duas")
    void characteristicRenamedTwice() {
        var admin = adminBrowser();
        var typeId = createType(admin, "Inversor Renomeia", "Potência", "KILOWATT");

        var first = admin.put("/catalog/equipment-types/" + typeId, typeBody("Inversor Renomeia", "Potência nominal", "KILOWATT"));
        var second = admin.put("/catalog/equipment-types/" + typeId, typeBody("Inversor Renomeia", "Potência máxima", "KILOWATT"));

        assertThat(first.status()).isEqualTo(204);
        assertThat(second.status()).isEqualTo(204);
        assertThat(typeOf(admin, typeId).get("characteristics").get(0).get("name").asText()).isEqualTo("Potência máxima");
    }

    @Test
    @Order(4)
    @DisplayName("status desconhecido nas listas é 400")
    void unknownStatusIsBadRequest() {
        var admin = adminBrowser();

        assertThat(admin.get("/catalog/equipment-models?status=FOO").status()).isEqualTo(400);
        assertThat(admin.get("/catalog/equipment-types?status=FOO").status()).isEqualTo(400);
        assertThat(admin.get("/catalog/manufacturers?status=FOO").status()).isEqualTo(400);
    }

    // --- apoio ---------------------------------------------------------------------------------

    /** O Administrador logado e com a senha já trocada, seja qual for a ordem dos cenários. */
    private Browser adminBrowser() {
        var admin = browser("admin-pc");

        admin.get("/auth/me");

        if (admin.post("/auth/login", Map.of("email", ADMIN_EMAIL, "password", ADMIN_PASSWORD)).status() == 200)
            return admin;

        admin.post("/auth/login", Map.of("email", ADMIN_EMAIL, "password", ADMIN_INITIAL_PASSWORD));
        admin.put("/auth/password", Map.of("currentPassword", ADMIN_INITIAL_PASSWORD, "newPassword", ADMIN_PASSWORD));

        return admin;
    }

    /** Convida um Técnico pelo fluxo do IAM e devolve o navegador dele, já com a sessão do aceite. */
    private Browser technicianBrowser(
            Browser admin,
            String email
    ) {
        var invite = admin.post("/users", Map.of("name", "Tecnico Catalogo", "email", email, "role", "TECHNICIAN"));

        assertThat(invite.status()).isEqualTo(201);

        var technician = browser("tech-phone");

        technician.get("/auth/me");

        var accepted = technician.post(
                "/auth/invitation/accept",
                Map.of("token", emails.linkTokenSentTo(email), "password", TECHNICIAN_PASSWORD)
        );

        assertThat(accepted.status()).isEqualTo(200);

        return technician;
    }

    private UUID createManufacturer(
            Browser admin,
            String name
    ) {
        return created(admin.post("/catalog/manufacturers", Map.of("name", name)));
    }

    private UUID createType(
            Browser admin,
            String name,
            String characteristic,
            String unit
    ) {
        return created(admin.post("/catalog/equipment-types", typeBody(name, characteristic, unit)));
    }

    private UUID createModel(
            Browser admin,
            UUID manufacturerId,
            UUID typeId,
            UUID definitionId,
            String name,
            String amount
    ) {
        return created(admin.post(
                "/catalog/equipment-models",
                Map.of(
                        "manufacturerId", manufacturerId,
                        "typeId", typeId,
                        "name", name,
                        "values", List.of(Map.of("definitionId", definitionId, "amount", amount))
                )
        ));
    }

    private static Map<String, Object> typeBody(
            String name,
            String characteristic,
            String unit
    ) {
        return Map.of("name", name, "characteristic", Map.of("name", characteristic, "unit", unit, "required", true));
    }

    private static UUID created(Reply reply) {
        assertThat(reply.status()).as(reply.raw()).isEqualTo(201);

        return UUID.fromString(reply.body().get("id").asText());
    }

    private JsonNode typeOf(
            Browser admin,
            UUID typeId
    ) {
        for (var type : admin.get("/catalog/equipment-types?size=100").body().get("content"))
            if (typeId.toString().equals(type.get("id").asText()))
                return type;

        throw new AssertionError("tipo não listado: " + typeId);
    }

    private UUID definitionIdOf(
            Browser admin,
            UUID typeId
    ) {
        return UUID.fromString(typeOf(admin, typeId).get("characteristics").get(0).get("id").asText());
    }

    private Browser browser(String device) {
        return new Browser(http, port, secrets, device);
    }
}
