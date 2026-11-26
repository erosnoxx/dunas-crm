package com.crm.iam.domain.entities;

import com.crm.common.domain.exceptions.DomainException;
import com.crm.iam.domain.events.UserDeactivated;
import com.crm.iam.domain.events.UserRoleChanged;
import com.crm.iam.domain.vos.PasswordHash;
import com.crm.shared.domain.enums.Role;
import com.crm.iam.domain.vos.UserName;
import com.crm.iam.domain.enums.UserStatus;
import com.crm.shared.domain.vos.contact.Email;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("User")
class UserTest {

    private static final Instant NOW = Instant.parse("2026-11-19T12:00:00Z");

    private static final UUID ADMIN_ID = UUID.fromString("00000000-0000-0000-0000-0000000000aa");

    private static final PasswordHash HASH =
            PasswordHash.of("$argon2id$v=19$m=19456,t=2,p=1$c29tZXNhbHQ$aGFzaGhhc2hoYXNo");

    private static final PasswordHash OTHER_HASH =
            PasswordHash.of("$argon2id$v=19$m=19456,t=2,p=1$b3RoZXI$b3RoZXJoYXNo");

    private static User invited() {
        return User.invite(
                UserName.of("Maria Souza"),
                Email.of("Maria@Dunas.com"),
                Role.COMMERCIAL,
                NOW
        );
    }

    private static User active() {
        var user = invited();

        user.acceptInvitation(HASH);
        user.pullEvents();

        return user;
    }

    private static User deactivated(boolean withPassword) {
        var user = withPassword ? active() : invited();

        user.deactivate(ADMIN_ID, NOW);
        user.pullEvents();

        return user;
    }

    private static void assertCode(Runnable action, String code) {
        assertThatThrownBy(action::run)
                .isInstanceOf(DomainException.class)
                .satisfies(e -> assertThat(((DomainException) e).getCode()).contains(code));
    }

    @Test
    @DisplayName("convite cria a conta INVITED, sem senha, sem versão e sem evento")
    void inviteCreatesInvitedUser() {
        var user = invited();

        assertThat(user.getId()).isNotNull();
        assertThat(user.getName()).isEqualTo(UserName.of("Maria Souza"));
        assertThat(user.getEmail().getValue()).isEqualTo("maria@dunas.com");
        assertThat(user.getRole()).isEqualTo(Role.COMMERCIAL);
        assertThat(user.getStatus()).isEqualTo(UserStatus.INVITED);
        assertThat(user.getPasswordHash()).isEmpty();
        assertThat(user.isMustChangePassword()).isFalse();
        assertThat(user.getVersion()).isEmpty();
        assertThat(user.pullEvents()).isEmpty();
    }

    @Test
    @DisplayName("convites geram ids diferentes")
    void inviteGeneratesNewIds() {
        assertThat(invited().getId()).isNotEqualTo(invited().getId());
    }

    @Test
    @DisplayName("aceitar o convite ativa a conta com o hash")
    void acceptInvitationActivates() {
        var user = invited();

        user.acceptInvitation(HASH);

        assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(user.getPasswordHash()).contains(HASH);
    }

    @Test
    @DisplayName("aceitar o convite de conta ACTIVE ou DEACTIVATED falha com user_not_invited")
    void acceptInvitationOutsideInvited() {
        assertCode(() -> active().acceptInvitation(HASH), "user_not_invited");
        assertCode(() -> deactivated(true).acceptInvitation(HASH), "user_not_invited");
    }

    @Test
    @DisplayName("trocar a senha troca o hash e zera mustChangePassword")
    void changePasswordReplacesHashAndClearsFlag() {
        var user = User.reconstitute(
                UUID.randomUUID(),
                1L,
                UserName.of("Admin"),
                Email.of("admin@dunas.com"),
                Role.ADMINISTRATOR,
                UserStatus.ACTIVE,
                HASH,
                true
        );

        user.changePassword(OTHER_HASH);

        assertThat(user.getPasswordHash()).contains(OTHER_HASH);
        assertThat(user.isMustChangePassword()).isFalse();
    }

    @Test
    @DisplayName("redefinir a senha troca o hash")
    void resetPasswordReplacesHash() {
        var user = active();

        user.resetPassword(OTHER_HASH);

        assertThat(user.getPasswordHash()).contains(OTHER_HASH);
    }

    @Test
    @DisplayName("trocar ou redefinir a senha fora de ACTIVE falha com user_not_active")
    void passwordChangesRequireActive() {
        assertCode(() -> invited().changePassword(OTHER_HASH), "user_not_active");
        assertCode(() -> invited().resetPassword(OTHER_HASH), "user_not_active");
        assertCode(() -> deactivated(true).changePassword(OTHER_HASH), "user_not_active");
        assertCode(() -> deactivated(true).resetPassword(OTHER_HASH), "user_not_active");
    }

    @Test
    @DisplayName("mudar o papel de outro usuário registra UserRoleChanged")
    void changeRoleRegistersEvent() {
        var user = active();

        user.changeRole(ADMIN_ID, Role.TECHNICIAN, NOW);

        assertThat(user.getRole()).isEqualTo(Role.TECHNICIAN);
        assertThat(user.pullEvents()).containsExactly(new UserRoleChanged(user.getId(), NOW));
    }

    @Test
    @DisplayName("mudar o próprio papel falha com cannot_change_own_role")
    void changeOwnRole() {
        var user = active();

        assertCode(
                () -> user.changeRole(user.getId(), Role.ADMINISTRATOR, NOW),
                "cannot_change_own_role"
        );
        assertThat(user.getRole()).isEqualTo(Role.COMMERCIAL);
    }

    @Test
    @DisplayName("mudar o papel de conta fora de ACTIVE falha com user_not_active")
    void changeRoleRequiresActive() {
        assertCode(() -> invited().changeRole(ADMIN_ID, Role.TECHNICIAN, NOW), "user_not_active");
        assertCode(() -> deactivated(true).changeRole(ADMIN_ID, Role.TECHNICIAN, NOW), "user_not_active");
    }

    @Test
    @DisplayName("desativar conta INVITED ou ACTIVE registra UserDeactivated com a hora recebida")
    void deactivateRegistersEvent() {
        var invited = invited();
        var active = active();

        invited.deactivate(ADMIN_ID, NOW);
        active.deactivate(ADMIN_ID, NOW);

        assertThat(invited.getStatus()).isEqualTo(UserStatus.DEACTIVATED);
        assertThat(active.getStatus()).isEqualTo(UserStatus.DEACTIVATED);
        assertThat(invited.pullEvents()).containsExactly(new UserDeactivated(invited.getId(), NOW));
        assertThat(active.pullEvents()).containsExactly(new UserDeactivated(active.getId(), NOW));
    }

    @Test
    @DisplayName("desativar a si mesmo falha com cannot_deactivate_self")
    void deactivateSelf() {
        var user = active();

        assertCode(() -> user.deactivate(user.getId(), NOW), "cannot_deactivate_self");
        assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
    }

    @Test
    @DisplayName("desativar conta já desativada falha com user_deactivated")
    void deactivateTwice() {
        assertCode(() -> deactivated(true).deactivate(ADMIN_ID, NOW), "user_deactivated");
    }

    @Test
    @DisplayName("reativar conta que tinha senha volta para ACTIVE")
    void reactivateWithPassword() {
        var user = deactivated(true);

        user.reactivate();

        assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
    }

    @Test
    @DisplayName("reativar conta que nunca definiu senha volta para INVITED")
    void reactivateWithoutPassword() {
        var user = deactivated(false);

        user.reactivate();

        assertThat(user.getStatus()).isEqualTo(UserStatus.INVITED);
    }

    @Test
    @DisplayName("reativar conta fora de DEACTIVATED falha com user_not_deactivated")
    void reactivateOutsideDeactivated() {
        assertCode(() -> invited().reactivate(), "user_not_deactivated");
        assertCode(() -> active().reactivate(), "user_not_deactivated");
    }

    @Test
    @DisplayName("marcar troca de senha obrigatória liga mustChangePassword")
    void markPasswordChangeRequired() {
        var user = active();

        user.markPasswordChangeRequired();

        assertThat(user.isMustChangePassword()).isTrue();
    }

    @Test
    @DisplayName("reconstituir carrega a versão e não registra evento")
    void reconstituteKeepsVersionWithoutEvents() {
        var id = UUID.randomUUID();

        var user = User.reconstitute(
                id,
                4L,
                UserName.of("Maria Souza"),
                Email.of("maria@dunas.com"),
                Role.TECHNICIAN,
                UserStatus.ACTIVE,
                HASH,
                false
        );

        assertThat(user.getId()).isEqualTo(id);
        assertThat(user.getVersion()).isEqualTo(Optional.of(4L));
        assertThat(user.pullEvents()).isEmpty();
    }

    @Test
    @DisplayName("reconstituir ACTIVE sem hash viola a invariante")
    void reconstituteActiveWithoutHash() {
        assertThatThrownBy(() -> User.reconstitute(
                UUID.randomUUID(),
                1L,
                UserName.of("Maria Souza"),
                Email.of("maria@dunas.com"),
                Role.TECHNICIAN,
                UserStatus.ACTIVE,
                null,
                false
        )).isInstanceOf(DomainException.class);
    }

    @Test
    @DisplayName("reconstituir INVITED com hash viola a invariante")
    void reconstituteInvitedWithHash() {
        assertThatThrownBy(() -> User.reconstitute(
                UUID.randomUUID(),
                1L,
                UserName.of("Maria Souza"),
                Email.of("maria@dunas.com"),
                Role.TECHNICIAN,
                UserStatus.INVITED,
                HASH,
                false
        )).isInstanceOf(DomainException.class);
    }

    @Test
    @DisplayName("pullEvents devolve os eventos e esvazia a lista")
    void pullEventsDrains() {
        var user = active();
        user.deactivate(ADMIN_ID, NOW);

        assertThat(user.pullEvents()).hasSize(1);
        assertThat(user.pullEvents()).isEmpty();
    }
}
