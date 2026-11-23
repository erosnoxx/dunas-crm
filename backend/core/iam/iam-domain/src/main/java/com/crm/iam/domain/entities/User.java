package com.crm.iam.domain.entities;

import com.crm.common.domain.entities.AggregateRoot;
import com.crm.common.domain.exceptions.DomainException;
import com.crm.iam.domain.events.UserDeactivated;
import com.crm.iam.domain.events.UserRoleChanged;
import com.crm.iam.domain.vos.PasswordHash;
import com.crm.shared.domain.enums.Role;
import com.crm.iam.domain.vos.UserName;
import com.crm.iam.domain.enums.UserStatus;
import com.crm.shared.domain.vos.contact.Email;
import lombok.Getter;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Conta de acesso ao sistema: identidade, papel, situação e credencial.
 *
 * <p>O ciclo de vida (convidada, ativa, desativada) é um enum com guardas nos métodos, sem máquina
 * de estados: são três estados e poucas transições. {@code ACTIVE} sempre tem senha e
 * {@code INVITED} nunca tem; a invariante vale também ao reconstituir do banco. O aggregate só
 * vê o {@link PasswordHash}, nunca a senha em texto.
 */
@Getter
public class User extends AggregateRoot {

    private final UserName name;

    private final Email email;

    private Role role;

    private UserStatus status;

    private PasswordHash passwordHash;

    private boolean mustChangePassword;

    private User(
            UUID id,
            Long version,
            UserName name,
            Email email,
            Role role,
            UserStatus status,
            PasswordHash passwordHash,
            boolean mustChangePassword
    ) {
        super(id, version);

        this.name = name;
        this.email = email;
        this.role = role;
        this.status = status;
        this.passwordHash = passwordHash;
        this.mustChangePassword = mustChangePassword;
    }

    /** Cria a conta convidada: ainda sem senha, que o convidado define ao aceitar o convite. */
    public static User invite(
            UserName name,
            Email email,
            Role role,
            Instant now
    ) {
        return new User(
                UUID.randomUUID(),
                null,
                name,
                email,
                role,
                UserStatus.INVITED,
                null,
                false
        );
    }

    public static User reconstitute(
            UUID id,
            Long version,
            UserName name,
            Email email,
            Role role,
            UserStatus status,
            PasswordHash passwordHash,
            boolean mustChangePassword
    ) {
        if (status == UserStatus.ACTIVE && passwordHash == null)
            throw new DomainException("conta ativa precisa ter senha");

        if (status == UserStatus.INVITED && passwordHash != null)
            throw new DomainException("conta convidada não pode ter senha");

        return new User(
                id,
                version,
                name,
                email,
                role,
                status,
                passwordHash,
                mustChangePassword
        );
    }

    /** Ausente enquanto a conta está convidada. */
    public Optional<PasswordHash> getPasswordHash() {
        return Optional.ofNullable(passwordHash);
    }

    public void acceptInvitation(PasswordHash newHash) {
        if (status != UserStatus.INVITED)
            throw new DomainException("user_not_invited", "este usuário não está com convite pendente");

        this.passwordHash = newHash;
        this.status = UserStatus.ACTIVE;
    }

    public void changePassword(PasswordHash newHash) {
        requireActive();

        this.passwordHash = newHash;
        this.mustChangePassword = false;
    }

    public void resetPassword(PasswordHash newHash) {
        requireActive();

        this.passwordHash = newHash;
    }

    public void changeRole(
            UUID actorId,
            Role newRole,
            Instant now
    ) {
        if (getId().equals(actorId))
            throw new DomainException("cannot_change_own_role", "você não pode mudar o seu próprio papel");

        requireActive();

        this.role = newRole;

        registerEvent(new UserRoleChanged(getId(), now));
    }

    public void deactivate(UUID actorId, Instant now) {
        if (getId().equals(actorId))
            throw new DomainException("cannot_deactivate_self", "você não pode desativar a sua própria conta");

        if (status == UserStatus.DEACTIVATED)
            throw new DomainException("user_deactivated", "este usuário já está desativado");

        this.status = UserStatus.DEACTIVATED;

        registerEvent(new UserDeactivated(getId(), now));
    }

    /** Devolve a conta ao estado de antes da desativação: ativa se tem senha, convidada se não. */
    public void reactivate() {
        if (status != UserStatus.DEACTIVATED)
            throw new DomainException("user_not_deactivated", "este usuário não está desativado");

        this.status = passwordHash == null ? UserStatus.INVITED : UserStatus.ACTIVE;
    }

    /** Só o seeder do primeiro Administrador usa: obriga a trocar a senha inicial. */
    public void markPasswordChangeRequired() {
        this.mustChangePassword = true;
    }

    private void requireActive() {
        if (status != UserStatus.ACTIVE)
            throw new DomainException("user_not_active", "este usuário não está ativo");
    }
}
