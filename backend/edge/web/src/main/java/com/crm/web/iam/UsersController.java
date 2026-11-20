package com.crm.web.iam;

import com.crm.common.application.pages.Page;
import com.crm.common.application.pages.Pageable;
import com.crm.iam.application.Actor;
import com.crm.iam.application.schemas.SessionSummary;
import com.crm.iam.application.schemas.UserDetail;
import com.crm.iam.application.schemas.UserFilter;
import com.crm.iam.application.schemas.UserSummary;
import com.crm.iam.application.usecases.commands.ChangeUserRole;
import com.crm.iam.application.usecases.commands.DeactivateUser;
import com.crm.iam.application.usecases.commands.InviteUser;
import com.crm.iam.application.usecases.commands.ReactivateUser;
import com.crm.iam.application.usecases.commands.ResendInvitation;
import com.crm.iam.application.usecases.commands.RevokeAllSessions;
import com.crm.iam.application.usecases.queries.GetUser;
import com.crm.iam.application.usecases.queries.ListSessions;
import com.crm.iam.application.usecases.queries.ListUsers;
import com.crm.shared.application.schemas.response.UUIDResponse;
import com.crm.web.iam.requests.ChangeRoleRequest;
import com.crm.web.iam.requests.InviteUserRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Gestão de usuários e das sessões deles. A checagem de papel é do use case (ADR-5), nunca daqui.
 *
 * <p>O filtro da lista é ligado direto de {@code role}, {@code status} e {@code text}: o Spring
 * converte o texto no enum, então o controller não conhece o domínio e valor desconhecido vira 400.
 */
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UsersController {

    private static final int MAX_PAGE_SIZE = 100;

    private final InviteUser inviteUser;
    private final ResendInvitation resendInvitation;
    private final ChangeUserRole changeUserRole;
    private final DeactivateUser deactivateUser;
    private final ReactivateUser reactivateUser;
    private final RevokeAllSessions revokeAllSessions;
    private final ListUsers listUsers;
    private final GetUser getUser;
    private final ListSessions listSessions;

    @PostMapping
    public ResponseEntity<UUIDResponse> invite(
            Actor actor,
            @Valid @RequestBody InviteUserRequest body
    ) {
        var created = inviteUser.execute(new InviteUser.Params(
                actor,
                body.name(),
                body.email(),
                body.role()
        ));

        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    public Page<UserSummary> list(
            Actor actor,
            @ModelAttribute UserFilter filter,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        var pageable = Pageable.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE));

        return listUsers.execute(new ListUsers.Params(
                actor,
                filter,
                pageable
        ));

    }

    @GetMapping("/{id}")
    public UserDetail detail(
            Actor actor,
            @PathVariable UUID id
    ) {
        return getUser.execute(new GetUser.Params(actor, id));
    }

    @PostMapping("/{id}/invitation")
    public ResponseEntity<Void> resendInvitation(
            Actor actor,
            @PathVariable UUID id
    ) {
        resendInvitation.execute(new ResendInvitation.Params(actor, id));

        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/role")
    public ResponseEntity<Void> changeRole(
            Actor actor,
            @PathVariable UUID id,
            @Valid @RequestBody ChangeRoleRequest body
    ) {
        changeUserRole.execute(new ChangeUserRole.Params(
                actor,
                id,
                body.role()
        ));

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/deactivation")
    public ResponseEntity<Void> deactivate(
            Actor actor,
            @PathVariable UUID id
    ) {
        deactivateUser.execute(new DeactivateUser.Params(actor, id));

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/reactivation")
    public ResponseEntity<Void> reactivate(
            Actor actor,
            @PathVariable UUID id
    ) {
        reactivateUser.execute(new ReactivateUser.Params(actor, id));

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/sessions")
    public List<SessionSummary> sessions(
            Actor actor,
            @PathVariable UUID id
    ) {
        return listSessions.execute(new ListSessions.Params(actor, id));
    }

    @DeleteMapping("/{id}/sessions")
    public ResponseEntity<Void> revokeSessions(
            Actor actor,
            @PathVariable UUID id,
            @RequestParam(defaultValue = "false") boolean keepCurrent
    ) {
        revokeAllSessions.execute(new RevokeAllSessions.Params(
                actor,
                id,
                keepCurrent
        ));

        return ResponseEntity.noContent().build();
    }
}
