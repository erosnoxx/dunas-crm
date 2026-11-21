package com.crm.web.iam;

import com.crm.shared.application.Actor;
import com.crm.iam.application.schemas.CurrentUser;
import com.crm.iam.application.schemas.SessionResponse;
import com.crm.iam.application.usecases.commands.AcceptInvitation;
import com.crm.iam.application.usecases.commands.ChangePassword;
import com.crm.iam.application.usecases.commands.Login;
import com.crm.iam.application.usecases.commands.Logout;
import com.crm.iam.application.usecases.commands.RequestPasswordReset;
import com.crm.iam.application.usecases.commands.ResetPassword;
import com.crm.iam.application.usecases.queries.GetCurrentUser;
import com.crm.web.iam.requests.AcceptInvitationRequest;
import com.crm.web.iam.requests.ChangePasswordRequest;
import com.crm.web.iam.requests.ForgotPasswordRequest;
import com.crm.web.iam.requests.LoginRequest;
import com.crm.web.iam.requests.ResetPasswordRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

/**
 * Entrada, saída e senha: login, aceite de convite, logout, esqueci/redefinir/trocar senha e
 * quem sou eu.
 *
 * <p>O token da sessão sai só no cookie {@code HttpOnly} (architecture.md §14); o corpo leva
 * {@link SessionView}. O formato do aplicativo (token no corpo) é do plano do Capacitor
 * (architecture.md §14). Nenhuma exception é capturada aqui: o aspect de {@code @UseCase} e o
 * {@code RestExceptionHandler} traduzem.
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final String DEVICE_HEADER = "X-Device-Name";
    private static final String UNKNOWN_DEVICE = "Aparelho desconhecido";
    private static final int DEVICE_MAX_LENGTH = 80;

    private final Login login;
    private final Logout logout;
    private final AcceptInvitation acceptInvitation;
    private final RequestPasswordReset requestPasswordReset;
    private final ResetPassword resetPassword;
    private final ChangePassword changePassword;
    private final GetCurrentUser getCurrentUser;
    private final SessionCookie sessionCookie;

    @PostMapping("/login")
    public ResponseEntity<SessionView> login(
            @Valid @RequestBody LoginRequest body,
            HttpServletRequest request
    ) {
        var session = login.execute(new Login.Params(
                body.email(),
                body.password(),
                deviceOf(request),
                request.getRemoteAddr()
        ));

        return opened(session);
    }

    @PostMapping("/invitation/accept")
    public ResponseEntity<SessionView> acceptInvitation(
            @Valid @RequestBody AcceptInvitationRequest body,
            HttpServletRequest request
    ) {
        var session = acceptInvitation.execute(new AcceptInvitation.Params(
                body.token(),
                body.password(),
                deviceOf(request)
        ));

        return opened(session);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(Actor actor) {
        logout.execute(actor);

        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, sessionCookie.clear())
                .build();
    }

    @PostMapping("/password/forgot")
    public ResponseEntity<Void> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest body,
            HttpServletRequest request
    ) {
        requestPasswordReset.execute(new RequestPasswordReset.Params(body.email(), request.getRemoteAddr()));

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/password/reset")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest body) {
        resetPassword.execute(new ResetPassword.Params(body.token(), body.password()));

        return ResponseEntity.noContent().build();
    }

    @PutMapping("/password")
    public ResponseEntity<Void> changePassword(
            Actor actor,
            @Valid @RequestBody ChangePasswordRequest body
    ) {
        changePassword.execute(new ChangePassword.Params(
                actor,
                body.currentPassword(),
                body.newPassword()
        ));

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public CurrentUser me(Actor actor) {
        return getCurrentUser.execute(actor);
    }

    private ResponseEntity<SessionView> opened(SessionResponse session) {
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, sessionCookie.issue(session.token()))
                .body(SessionView.of(session));
    }

    private static String deviceOf(HttpServletRequest request) {
        var device = Optional.ofNullable(request.getHeader(DEVICE_HEADER))
                .filter(value -> !value.isBlank())
                .or(() -> Optional.ofNullable(request.getHeader(HttpHeaders.USER_AGENT)).filter(value -> !value.isBlank()))
                .orElse(UNKNOWN_DEVICE);

        return device.substring(0, Math.min(device.length(), DEVICE_MAX_LENGTH));
    }
}
