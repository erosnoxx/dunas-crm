package com.crm;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.crm.common.application.email.EmailMessage;
import com.crm.common.application.email.EmailSenderService;
import com.crm.common.application.exceptions.ExternalServiceException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;

import java.net.URI;
import java.net.URLDecoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * O IAM inteiro no contexto real da aplicação: Tomcat de verdade numa porta aleatória, Postgres e
 * Redis do Testcontainers (exige Docker), o seeder criando o Administrador e um cliente HTTP que
 * guarda os cookies como um navegador e devolve o {@code X-XSRF-TOKEN} à mão.
 *
 * <p>O e-mail é capturado por um {@link EmailSenderService} falso e o relógio é um {@link Clock}
 * que o teste adianta. O RabbitMQ não sobe: o IAM não publica integration event, e só o health do
 * broker e do SMTP é desligado aqui, sem mexer na configuração de produção.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "CRM_ADMIN_NAME=Administradora Inicial",
                "CRM_ADMIN_EMAIL=" + IamEndToEndTest.ADMIN_EMAIL,
                "CRM_ADMIN_PASSWORD=" + IamEndToEndTest.ADMIN_INITIAL_PASSWORD,
                "spring.mail.host=localhost",
                "crm.email.from=nao-responda@dunas.test",
                "management.health.rabbit.enabled=false",
                "management.health.mail.enabled=false",
                "crm.outbox.poll-delay=1h"
        }
)
@Import(IamEndToEndTest.TestBeans.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("IAM ponta a ponta no servidor real")
class IamEndToEndTest {

    static final String ADMIN_EMAIL = "admin@dunas.test";
    static final String ADMIN_INITIAL_PASSWORD = "Inicial#2026";
    static final String ADMIN_PASSWORD = "Trocada#2026";
    static final String USER_PASSWORD = "Tecnico#2026";

    private static final Instant START = Instant.parse("2026-11-20T12:00:00Z");
    private static final Pattern LINK_TOKEN = Pattern.compile("token=([A-Za-z0-9_%\\-]+)");
    private static final ObjectMapper JSON = new ObjectMapper();

    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16");

    @ServiceConnection(name = "redis")
    static final GenericContainer<?> REDIS = new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

    static {
        POSTGRES.start();
        REDIS.start();
    }

    @TestConfiguration
    static class TestBeans {

        @Bean
        @Primary
        MutableClock testClock() {
            return new MutableClock(START);
        }

        @Bean
        @Primary
        CapturingEmailSender capturingEmailSender() {
            return new CapturingEmailSender();
        }
    }

    @LocalServerPort
    private int port;

    @Autowired
    private MutableClock clock;

    @Autowired
    private CapturingEmailSender emails;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private StringRedisTemplate redis;

    private final HttpClient http = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build();
    private final ListAppender<ILoggingEvent> logs = new ListAppender<>();
    private final Set<String> secrets = new HashSet<>();

    /** Cada cenário começa sem contadores de tentativa nem sessão em cache, e sem e-mail capturado. */
    @BeforeEach
    void setUp() {
        redis.getConnectionFactory().getConnection().serverCommands().flushAll();

        emails.reset();

        logs.start();
        rootLogger().addAppender(logs);
    }

    /** Nenhum segredo visto no cenário (senha, token de sessão, segredo de link) pode ter ido ao log. */
    @AfterEach
    void noSecretInLogs() {
        rootLogger().detachAppender(logs);

        for (var text : logged())
            for (var secret : secrets)
                assertThat(text).as("log sem segredo").doesNotContain(secret);
    }

    // --- cenários --------------------------------------------------------------------------------

    @Test
    @Order(1)
    @DisplayName("fluxo completo: troca obrigatória, convite, sessões, revogação, desativação e reativação")
    void fullFlow() {
        var admin = browser("admin-pc");

        var anonymous = admin.get("/auth/me");

        assertThat(anonymous.status()).isEqualTo(401);
        assertThat(anonymous.code()).isEqualTo("session_invalid");
        assertThat(anonymous.setCookie("XSRF-TOKEN")).isPresent();
        assertThat(anonymous.setCookie("XSRF-TOKEN").get()).doesNotContainIgnoringCase("HttpOnly");

        var login = admin.post("/auth/login", Map.of("email", ADMIN_EMAIL, "password", ADMIN_INITIAL_PASSWORD));

        assertThat(login.status()).isEqualTo(200);
        assertThat(login.body().get("mustChangePassword").asBoolean()).isTrue();
        assertThat(login.body().has("token")).isFalse();

        var sessionCookie = login.setCookie("crm_session").orElseThrow();

        assertThat(sessionCookie).contains("HttpOnly").contains("SameSite=Lax").contains("Path=/");
        assertThat(sessionCookie).doesNotContain("Secure");

        assertThat(admin.get("/users").code()).isEqualTo("password_change_required");
        assertThat(admin.get("/users").status()).isEqualTo(403);

        var change = admin.put(
                "/auth/password",
                Map.of("currentPassword", ADMIN_INITIAL_PASSWORD, "newPassword", ADMIN_PASSWORD)
        );

        assertThat(change.status()).isEqualTo(204);

        // sem a remoção do cache pela troca, o mustChangePassword velho valeria até o TTL de 60 s
        assertThat(admin.get("/users").status()).isEqualTo(200);

        var technicianEmail = "tecnico.fluxo@dunas.test";
        var invite = admin.post("/users", Map.of("name", "Tecnico Fluxo", "email", technicianEmail, "role", "TECHNICIAN"));

        assertThat(invite.status()).isEqualTo(201);

        var technicianId = UUID.fromString(invite.body().get("id").asText());
        var invitationToken = linkTokenSentTo(technicianEmail);

        var phone = browser("tech-phone");

        phone.get("/auth/me");

        var accepted = phone.post("/auth/invitation/accept", Map.of("token", invitationToken, "password", USER_PASSWORD));

        assertThat(accepted.status()).isEqualTo(200);
        assertThat(accepted.body().get("mustChangePassword").asBoolean()).isFalse();
        assertThat(phone.get("/auth/me").status()).isEqualTo(200);

        var laptop = browser("tech-laptop");

        laptop.get("/auth/me");

        assertThat(laptop.post("/auth/login", Map.of("email", technicianEmail, "password", USER_PASSWORD)).status())
                .isEqualTo(200);
        // põe a sessão no cache: a revogação precisa tirá-la de lá, não esperar o TTL
        assertThat(laptop.get("/auth/me").status()).isEqualTo(200);

        var sessions = admin.get("/users/" + technicianId + "/sessions");

        assertThat(sessions.status()).isEqualTo(200);
        assertThat(sessions.body()).hasSize(2);

        var laptopSessionId = sessionIdOf(sessions.body(), "tech-laptop");

        assertThat(admin.delete("/sessions/" + laptopSessionId).status()).isEqualTo(204);

        var revoked = laptop.get("/auth/me");

        assertThat(revoked.status()).isEqualTo(401);
        assertThat(revoked.code()).isEqualTo("session_invalid");
        assertThat(phone.get("/auth/me").status()).isEqualTo(200);

        assertThat(admin.post("/users/" + technicianId + "/deactivation", Map.of()).status()).isEqualTo(204);

        // o listener grava em REQUIRES_NEW depois do commit: sem isso nenhuma revogação seria gravada
        assertThat(activeSessionsOf(technicianId)).isZero();
        assertThat(phone.get("/auth/me").status()).isEqualTo(401);

        var deactivatedLogin = browser("tech-tablet");

        deactivatedLogin.get("/auth/me");

        var refused = deactivatedLogin.post("/auth/login", Map.of("email", technicianEmail, "password", USER_PASSWORD));

        assertThat(refused.status()).isEqualTo(401);
        assertThat(refused.code()).isEqualTo("invalid_credentials");

        assertThat(admin.post("/users/" + technicianId + "/reactivation", Map.of()).status()).isEqualTo(204);

        assertThat(phone.get("/auth/me").status()).isEqualTo(401);
        assertThat(deactivatedLogin.post("/auth/login", Map.of("email", technicianEmail, "password", USER_PASSWORD)).status())
                .isEqualTo(200);
        assertThat(deactivatedLogin.get("/auth/me").status()).isEqualTo(200);
    }

    @Test
    @Order(2)
    @DisplayName("limite de login: a sexta tentativa errada é 429, inclusive para e-mail inexistente, e o acerto só devolve a própria tentativa")
    void loginRateLimit() {
        var admin = adminBrowser();
        var email = "limite@dunas.test";

        activeUser(admin, email, "Usuario Limite");

        var attacker = browser("attacker");

        attacker.get("/auth/me");

        for (var attempt = 0; attempt < 4; attempt++)
            assertThat(attacker.post("/auth/login", Map.of("email", email, "password", "Errada#2026")).code())
                    .isEqualTo("invalid_credentials");

        // o acerto devolve só a tentativa dele: o contador fica em 4, não zera
        assertThat(attacker.post("/auth/login", Map.of("email", email, "password", USER_PASSWORD)).status()).isEqualTo(200);
        assertThat(attacker.post("/auth/login", Map.of("email", email, "password", "Errada#2026")).status()).isEqualTo(401);

        var limited = attacker.post("/auth/login", Map.of("email", email, "password", USER_PASSWORD));

        assertThat(limited.status()).isEqualTo(429);
        assertThat(limited.code()).isEqualTo("rate_limited");

        for (var attempt = 0; attempt < 5; attempt++)
            assertThat(attacker.post("/auth/login", Map.of("email", "ninguem@dunas.test", "password", "Errada#2026")).status())
                    .isEqualTo(401);

        var unknown = attacker.post("/auth/login", Map.of("email", "ninguem@dunas.test", "password", "Errada#2026"));

        assertThat(unknown.status()).isEqualTo(429);
        assertThat(unknown.code()).isEqualTo("rate_limited");
    }

    @Test
    @Order(3)
    @DisplayName("redefinição de senha: resposta igual para e-mail desconhecido, link de uso único que vence em 1 h e revoga as sessões")
    void passwordReset() {
        var admin = adminBrowser();
        var email = "redefine@dunas.test";

        activeUser(admin, email, "Usuario Redefine");

        var device = browser("reset-device");

        device.get("/auth/me");
        device.post("/auth/login", Map.of("email", email, "password", USER_PASSWORD));

        assertThat(device.get("/auth/me").status()).isEqualTo(200);

        var visitor = browser("visitor");

        visitor.get("/auth/me");

        var known = visitor.post("/auth/password/forgot", Map.of("email", email));
        var unknown = visitor.post("/auth/password/forgot", Map.of("email", "desconhecido@dunas.test"));

        assertThat(known.status()).isEqualTo(204);
        assertThat(unknown.status()).isEqualTo(204);
        assertThat(unknown.raw()).isEqualTo(known.raw());

        var token = linkTokenSentTo(email);

        assertThat(emails.sentTo("desconhecido@dunas.test")).isEmpty();

        var reset = visitor.post("/auth/password/reset", Map.of("token", token, "password", "Nova#Senha2026"));

        assertThat(reset.status()).isEqualTo(204);
        assertThat(activeSessionsOf(userIdOf(email))).isZero();
        assertThat(device.get("/auth/me").status()).isEqualTo(401);

        var reused = visitor.post("/auth/password/reset", Map.of("token", token, "password", "Outra#Senha2026"));

        assertThat(reused.status()).isEqualTo(422);
        assertThat(reused.code()).isEqualTo("link_used");
        assertThat(visitor.post("/auth/login", Map.of("email", email, "password", "Nova#Senha2026")).status()).isEqualTo(200);

        emails.reset();

        assertThat(visitor.post("/auth/password/forgot", Map.of("email", email)).status()).isEqualTo(204);

        var late = linkTokenSentTo(email);

        clock.advance(Duration.ofHours(1).plusMinutes(1));

        var expired = visitor.post("/auth/password/reset", Map.of("token", late, "password", "Tarde#Senha2026"));

        assertThat(expired.status()).isEqualTo(422);
        assertThat(expired.code()).isEqualTo("link_expired");
    }

    @Test
    @Order(4)
    @DisplayName("renovação aos 5 minutos: requisições paralelas com o mesmo cookie passam todas e a sessão é gravada uma vez")
    void parallelRenewal() {
        var admin = adminBrowser("renewal-device");
        var sessionId = jdbc.queryForObject("SELECT id FROM sessions WHERE device = 'renewal-device'", UUID.class);
        var versionBefore = versionOf(sessionId);

        assertThat(admin.get("/auth/me").status()).isEqualTo(200);

        clock.advance(Duration.ofMinutes(5).plusSeconds(1));

        var renewedAt = clock.instant();
        var requests = new ArrayList<CompletableFuture<HttpResponse<String>>>();

        for (var i = 0; i < 8; i++)
            requests.add(http.sendAsync(admin.request("GET", "/auth/me", null).build(), HttpResponse.BodyHandlers.ofString()));

        var statuses = requests.stream().map(CompletableFuture::join).map(HttpResponse::statusCode).toList();

        assertThat(statuses).containsOnly(200);
        assertThat(versionOf(sessionId)).isEqualTo(versionBefore + 1);
        assertThat(jdbc.queryForObject("SELECT last_used_at FROM sessions WHERE id = ?", Timestamp.class, sessionId).toInstant())
                .isEqualTo(renewedAt);
    }

    @Test
    @Order(5)
    @DisplayName("CSRF e cookies no servidor real: escrita só com cookie é 403 csrf_invalid e o health é público")
    void csrfAndCookies() {
        var admin = adminBrowser();

        var body = Map.of("name", "Forjado", "email", "forjado@dunas.test", "role", "TECHNICIAN");
        var forged = admin.send(admin.request("POST", "/users", body));

        assertThat(forged.status()).isEqualTo(403);
        assertThat(forged.code()).isEqualTo("csrf_invalid");

        var fresh = browser("fresh");
        var csrfCookie = fresh.get("/auth/me").setCookie("XSRF-TOKEN").orElseThrow();

        assertThat(csrfCookie).contains("SameSite=Lax").contains("Path=/").doesNotContain("Secure").doesNotContain("HttpOnly");

        // atrás do proxy com TLS (forward-headers-strategy=native), o Tomcat marca a requisição como segura
        var proxied = browser("proxied");
        var proxiedCookie = proxied.send(proxied.request("GET", "/auth/me", null).header("X-Forwarded-Proto", "https"))
                .setCookie("XSRF-TOKEN")
                .orElseThrow();

        assertThat(proxiedCookie).contains("Secure").contains("SameSite=Lax");

        var health = fresh.get("/actuator/health");

        assertThat(health.status()).isEqualTo(200);
        assertThat(health.body().get("status").asText()).isEqualTo("UP");
    }

    @Test
    @Order(6)
    @DisplayName("o e-mail sai no executor assíncrono e a falha de envio não derruba o convite")
    void asyncEmail() {
        var admin = adminBrowser();

        var invited = admin.post("/users", Map.of("name", "Comercial Async", "email", "async@dunas.test", "role", "COMMERCIAL"));

        assertThat(invited.status()).isEqualTo(201);

        linkTokenSentTo("async@dunas.test");

        assertThat(emails.sentTo("async@dunas.test").getFirst().thread()).startsWith("crm-side-effect-");

        emails.failing(true);

        var failing = admin.post("/users", Map.of("name", "Comercial Falha", "email", "falha@dunas.test", "role", "COMMERCIAL"));

        assertThat(failing.status()).isEqualTo(201);

        awaitUntil(() -> logged().stream().anyMatch(message -> message.startsWith("falha ao enviar o e-mail de INVITATION")));

        assertThat(emails.attempts()).isEqualTo(2);
    }

    @Test
    @Order(7)
    @DisplayName("convite com e-mail repetido é 409 e quem não é Administrador recebe 403 forbidden")
    void duplicateAndForbidden() {
        var admin = adminBrowser();
        var email = "comum@dunas.test";

        activeUser(admin, email, "Tecnico Comum");

        var duplicate = admin.post("/users", Map.of("name", "Outro", "email", email, "role", "TECHNICIAN"));

        assertThat(duplicate.status()).isEqualTo(409);
        assertThat(duplicate.code()).isEqualTo("email_already_registered");

        var technician = browser("comum");

        technician.get("/auth/me");
        technician.post("/auth/login", Map.of("email", email, "password", USER_PASSWORD));

        var forbidden = technician.get("/users");

        assertThat(forbidden.status()).isEqualTo(403);
        assertThat(forbidden.code()).isEqualTo("forbidden");
    }

    // --- apoio ---------------------------------------------------------------------------------

    /** O Administrador logado e com a senha já trocada, seja qual for a ordem dos cenários. */
    private Browser adminBrowser() {
        return adminBrowser("admin-pc");
    }

    private Browser adminBrowser(String device) {
        var admin = browser(device);

        admin.get("/auth/me");

        var login = admin.post("/auth/login", Map.of("email", ADMIN_EMAIL, "password", ADMIN_PASSWORD));

        if (login.status() == 200)
            return admin;

        admin.post("/auth/login", Map.of("email", ADMIN_EMAIL, "password", ADMIN_INITIAL_PASSWORD));
        admin.put("/auth/password", Map.of("currentPassword", ADMIN_INITIAL_PASSWORD, "newPassword", ADMIN_PASSWORD));

        return admin;
    }

    /** Convida e aceita o convite, deixando o usuário ativo com {@link #USER_PASSWORD}. */
    private UUID activeUser(
            Browser admin,
            String email,
            String name
    ) {
        var invite = admin.post("/users", Map.of("name", name, "email", email, "role", "TECHNICIAN"));

        assertThat(invite.status()).isEqualTo(201);

        var invitee = browser("convite");

        invitee.get("/auth/me");

        var token = linkTokenSentTo(email);
        var accepted = invitee.post("/auth/invitation/accept", Map.of("token", token, "password", USER_PASSWORD));

        assertThat(accepted.status()).isEqualTo(200);

        return UUID.fromString(invite.body().get("id").asText());
    }

    private String linkTokenSentTo(String email) {
        awaitUntil(() -> !emails.sentTo(email).isEmpty());

        var message = emails.sentTo(email).getLast().message();
        var matcher = LINK_TOKEN.matcher(message.text());

        assertThat(matcher.find()).as("link com token no e-mail").isTrue();

        var token = URLDecoder.decode(matcher.group(1), StandardCharsets.UTF_8);

        secrets.add(token);

        return token;
    }

    private static UUID sessionIdOf(
            JsonNode sessions,
            String device
    ) {
        for (var session : sessions)
            if (device.equals(session.get("device").asText()))
                return UUID.fromString(session.get("id").asText());

        throw new AssertionError("sessão do aparelho não listada: " + device);
    }

    private int activeSessionsOf(UUID userId) {
        return jdbc.queryForObject(
                "SELECT count(*) FROM sessions WHERE user_id = ? AND revoked_at IS NULL",
                Integer.class,
                userId
        );
    }

    private UUID userIdOf(String email) {
        return jdbc.queryForObject("SELECT id FROM users WHERE email = ?", UUID.class, email);
    }

    private long versionOf(UUID sessionId) {
        return jdbc.queryForObject("SELECT version FROM sessions WHERE id = ?", Long.class, sessionId);
    }

    private Browser browser(String device) {
        return new Browser(device);
    }

    /** Mensagem e causa de cada log capturado; o appender recebe de várias threads, então a cópia é sincronizada. */
    private List<String> logged() {
        synchronized (logs) {
            return logs.list.stream()
                    .map(event -> event.getFormattedMessage() + Optional.ofNullable(event.getThrowableProxy())
                            .map(proxy -> " " + proxy.getMessage())
                            .orElse(""))
                    .toList();
        }
    }

    private static Logger rootLogger() {
        return (Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
    }

    private static void awaitUntil(BooleanSupplier condition) {
        var deadline = System.nanoTime() + Duration.ofSeconds(10).toNanos();

        while (!condition.getAsBoolean()) {
            if (System.nanoTime() > deadline)
                throw new AssertionError("a condição não ficou verdadeira em 10 s");

            try {
                Thread.sleep(50);
            }
            catch (InterruptedException e) {
                Thread.currentThread().interrupt();

                throw new AssertionError("interrompido esperando a condição", e);
            }
        }
    }

    // --- cliente HTTP com cookies ---------------------------------------------------------------

    /**
     * Um navegador: guarda os cookies que o servidor manda, devolve-os em toda requisição e copia o
     * {@code XSRF-TOKEN} para o cabeçalho {@code X-XSRF-TOKEN} nas escritas, como o frontend fará.
     */
    private final class Browser {

        private final String device;
        private final Map<String, String> cookies = new ConcurrentHashMap<>();

        private Browser(String device) {
            this.device = device;
        }

        Reply get(String path) {
            return send(request("GET", path, null));
        }

        Reply post(
                String path,
                Object body
        ) {
            return send(withCsrf(request("POST", path, body)));
        }

        Reply put(
                String path,
                Object body
        ) {
            return send(withCsrf(request("PUT", path, body)));
        }

        Reply delete(String path) {
            return send(withCsrf(request("DELETE", path, null)));
        }

        HttpRequest.Builder request(
                String method,
                String path,
                Object body
        ) {
            var builder = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                    .header("X-Device-Name", device)
                    .header("Content-Type", "application/json")
                    .method(method, bodyOf(body));

            var cookieHeader = cookies.entrySet().stream()
                    .map(cookie -> cookie.getKey() + "=" + cookie.getValue())
                    .toList();

            if (!cookieHeader.isEmpty())
                builder.header("Cookie", String.join("; ", cookieHeader));

            return builder;
        }

        Reply send(HttpRequest.Builder request) {
            try {
                var response = http.send(request.build(), HttpResponse.BodyHandlers.ofString());
                var setCookies = response.headers().allValues("Set-Cookie");

                for (var header : setCookies)
                    keep(header);

                return new Reply(response.statusCode(), response.body(), setCookies);
            }
            catch (Exception e) {
                throw new AssertionError("falha na requisição HTTP", e);
            }
        }

        private HttpRequest.Builder withCsrf(HttpRequest.Builder request) {
            var token = cookies.get("XSRF-TOKEN");

            if (token != null)
                request.header("X-XSRF-TOKEN", token);

            return request;
        }

        private void keep(String header) {
            var pair = header.split(";", 2)[0];
            var name = pair.substring(0, pair.indexOf('='));
            var value = pair.substring(pair.indexOf('=') + 1);

            if ("crm_session".equals(name) && !value.isEmpty())
                secrets.add(value);

            if (value.isEmpty() || header.contains("Max-Age=0")) {
                cookies.remove(name);

                return;
            }

            cookies.put(name, value);
        }

        private HttpRequest.BodyPublisher bodyOf(Object body) {
            if (body == null)
                return HttpRequest.BodyPublishers.noBody();

            if (body instanceof Map<?, ?> map)
                for (var value : map.values())
                    if (value instanceof String text && text.contains("#"))
                        secrets.add(text);

            try {
                return HttpRequest.BodyPublishers.ofString(JSON.writeValueAsString(body));
            }
            catch (Exception e) {
                throw new AssertionError("corpo não serializável", e);
            }
        }
    }

    /** Resposta crua: status, corpo e os {@code Set-Cookie} exatamente como o Tomcat os mandou. */
    private record Reply(
            int status,
            String raw,
            List<String> setCookies
    ) {

        JsonNode body() {
            try {
                return JSON.readTree(raw);
            }
            catch (Exception e) {
                throw new AssertionError("corpo não é JSON: " + raw, e);
            }
        }

        String code() {
            if (raw == null || raw.isBlank())
                return null;

            return Optional.ofNullable(body().get("code")).map(JsonNode::asText).orElse(null);
        }

        Optional<String> setCookie(String name) {
            return setCookies.stream().filter(header -> header.startsWith(name + "=")).findFirst();
        }
    }

    // --- dublês ----------------------------------------------------------------------------------

    /** Relógio que o teste adianta; começa em {@link #START}. */
    static final class MutableClock extends Clock {

        private final AtomicReference<Instant> now;

        MutableClock(Instant start) {
            this.now = new AtomicReference<>(start);
        }

        void advance(Duration duration) {
            now.updateAndGet(current -> current.plus(duration));
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.of("America/Sao_Paulo");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now.get();
        }
    }

    /** Guarda os e-mails em vez de enviá-los, com a thread que os enviou; pode ser posto para falhar. */
    static final class CapturingEmailSender implements EmailSenderService {

        record Sent(EmailMessage message, String thread) {
        }

        private final List<Sent> sent = new CopyOnWriteArrayList<>();
        private final AtomicInteger attempts = new AtomicInteger();
        private volatile boolean failing;

        @Override
        public void send(EmailMessage message) {
            attempts.incrementAndGet();

            if (failing)
                throw new ExternalServiceException("servidor de e-mail fora do ar");

            sent.add(new Sent(message, Thread.currentThread().getName()));
        }

        List<Sent> sentTo(String email) {
            return sent.stream().filter(item -> item.message().to().equals(email)).toList();
        }

        int attempts() {
            return attempts.get();
        }

        void failing(boolean value) {
            failing = value;
        }

        void reset() {
            sent.clear();
            attempts.set(0);
            failing = false;
        }
    }
}
