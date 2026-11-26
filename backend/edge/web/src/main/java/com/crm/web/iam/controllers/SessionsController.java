package com.crm.web.iam.controllers;

import com.crm.shared.application.Actor;
import com.crm.iam.application.usecases.commands.RevokeSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** Encerra uma sessão pelo id, a do próprio ator ou, para o Administrador, a de outro usuário. */
@RestController
@RequestMapping("/sessions")
@RequiredArgsConstructor
public class SessionsController {

    private final RevokeSession revokeSession;

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> revoke(
            Actor actor,
            @PathVariable UUID id
    ) {
        revokeSession.execute(new RevokeSession.Params(actor, id));

        return ResponseEntity.noContent().build();
    }
}
