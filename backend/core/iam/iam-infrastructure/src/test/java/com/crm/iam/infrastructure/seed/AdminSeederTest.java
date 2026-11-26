package com.crm.iam.infrastructure.seed;

import com.crm.common.application.exceptions.InternalServerErrorException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.crm.iam.application.contracts.repositories.UserRepository;
import com.crm.iam.domain.entities.User;
import com.crm.shared.domain.enums.Role;
import com.crm.iam.domain.enums.UserStatus;
import com.crm.iam.infrastructure.security.Argon2PasswordHasherImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.util.PlaceholderResolutionException;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("AdminSeeder")
class AdminSeederTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-19T12:00:00Z"), ZoneOffset.UTC);

    private static final String PASSWORD = "Senha@Inicial1";

    private UserRepository userRepository;
    private AdminSeeder seeder;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);

        seeder = new AdminSeeder(userRepository, new Argon2PasswordHasherImpl(1024, 1, 1, 16, 32), CLOCK);
    }

    @Test
    @DisplayName("aponta o JSON do seed e o tipo do registro")
    void pointsToTheSeedResource() {
        assertThat(seeder.resourcePath()).isEqualTo("seed/iam-admin.json");
        assertThat(seeder.recordType()).isEqualTo(AdminSeed.class);
    }

    @Test
    @DisplayName("com usuários no banco já está semeado")
    void alreadySeededWhenThereAreUsers() {
        when(userRepository.count()).thenReturn(1L);

        assertThat(seeder.isAlreadySeeded()).isTrue();
    }

    @Test
    @DisplayName("sem usuários no banco ainda não está semeado")
    void notSeededWhenThereAreNoUsers() {
        when(userRepository.count()).thenReturn(0L);

        assertThat(seeder.isAlreadySeeded()).isFalse();
    }

    @Test
    @DisplayName("cria um Administrador ativo, que precisa trocar a senha, guardando o hash e nunca a senha")
    void createsTheActiveAdministratorWithTheHash() {
        seeder.seed(List.of(new AdminSeed("Eros Vieira", "Eros@Dunas.com", PASSWORD)));

        var saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());

        var user = saved.getValue();

        assertThat(user.getName().getValue()).isEqualTo("Eros Vieira");
        assertThat(user.getEmail().getValue()).isEqualTo("eros@dunas.com");
        assertThat(user.getRole()).isEqualTo(Role.ADMINISTRATOR);
        assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(user.isMustChangePassword()).isTrue();
        assertThat(user.getPasswordHash()).isPresent();
        assertThat(user.getPasswordHash().get().getValue()).startsWith("$argon2id$").doesNotContain(PASSWORD);
    }

    @Test
    @DisplayName("senha fraca derruba o seed com mensagem clara, sem repetir a senha, e não grava nada")
    void weakPasswordFailsWithoutEchoingIt() {
        var weak = "fraca";

        assertThatThrownBy(() -> seeder.seed(List.of(new AdminSeed("Eros Vieira", "eros@dunas.com", weak))))
                .isInstanceOf(InternalServerErrorException.class)
                .hasMessageContaining("administrador inicial")
                .hasMessageContaining("CRM_ADMIN_PASSWORD")
                .hasMessageNotContaining(weak);

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("o JSON usa as três variáveis sem default e vira AdminSeed depois de resolvidas")
    void jsonResolvesToAdminSeedAndHasNoDefaults() throws Exception {
        var raw = new String(
                new ClassPathResource(seeder.resourcePath()).getInputStream().readAllBytes(),
                StandardCharsets.UTF_8
        );

        assertThat(raw)
                .contains("${CRM_ADMIN_NAME}", "${CRM_ADMIN_EMAIL}", "${CRM_ADMIN_PASSWORD}")
                .doesNotContain(":}")
                .doesNotContainPattern("\\$\\{[A-Z_]+:");

        var environment = new MockEnvironment()
                .withProperty("CRM_ADMIN_NAME", "Eros")
                .withProperty("CRM_ADMIN_EMAIL", "eros@dunas.com")
                .withProperty("CRM_ADMIN_PASSWORD", PASSWORD);

        var type = new ObjectMapper().getTypeFactory().constructCollectionType(List.class, AdminSeed.class);
        List<AdminSeed> seeds = new ObjectMapper().readValue(environment.resolveRequiredPlaceholders(raw), type);

        assertThat(seeds).containsExactly(new AdminSeed("Eros", "eros@dunas.com", PASSWORD));

        assertThatThrownBy(() -> new MockEnvironment().resolveRequiredPlaceholders(raw))
                .isInstanceOf(PlaceholderResolutionException.class);
    }

    @Test
    @DisplayName("o toString do registro não mostra a senha")
    void seedToStringHidesThePassword() {
        assertThat(new AdminSeed("Eros", "eros@dunas.com", PASSWORD).toString()).doesNotContain(PASSWORD);
    }
}
