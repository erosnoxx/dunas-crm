package com.crm.boot.security;

import com.crm.common.application.exceptions.UnauthorizedException;
import com.crm.iam.application.usecases.commands.AuthenticateSession;
import com.crm.web.iam.ActorRequestAttribute;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Autentica a requisição pelo cookie {@code crm_session} (architecture.md §14) e impõe a sessão
 * restrita.
 *
 * <p>Sem cookie, segue sem autenticação: a cadeia responde 401 nas rotas protegidas. Com cookie,
 * chama {@link AuthenticateSession}; token que não vale vira 401 {@code session_invalid} em JSON,
 * e o cookie não é apagado aqui (o cliente decide quando sair). Sucesso deixa o
 * {@link com.crm.shared.application.Actor} no atributo {@link ActorRequestAttribute#NAME}, para o
 * resolvedor dos controllers, e no {@code SecurityContext}.
 *
 * <p>Rota pública nem consulta a sessão: um cookie vencido no navegador não pode impedir o login.
 *
 * <p>Conta com {@code mustChangePassword} só chama {@code PUT /auth/password},
 * {@code POST /auth/logout} e {@code GET /auth/me}; o resto é 403 {@code password_change_required}.
 *
 * <p>Roda fora de qualquer transação, como {@link AuthenticateSession} exige. O token nunca vai
 * para log nem para exception.
 */
class SessionAuthenticationFilter extends OncePerRequestFilter {

    static final String COOKIE_NAME = "crm_session";

    private static final Set<String> RESTRICTED_SESSION_ROUTES = Set.of(
            "PUT /auth/password",
            "POST /auth/logout",
            "GET /auth/me"
    );

    private final AuthenticateSession authenticateSession;
    private final RequestMatcher publicRoutes;
    private final ProblemJsonWriter problems;

    SessionAuthenticationFilter(
            AuthenticateSession authenticateSession,
            RequestMatcher publicRoutes,
            ProblemJsonWriter problems
    ) {
        this.authenticateSession = authenticateSession;
        this.publicRoutes = publicRoutes;
        this.problems = problems;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return publicRoutes.matches(request);
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain
    ) throws ServletException, IOException {
        var token = tokenOf(request);

        if (token.isEmpty()) {
            chain.doFilter(request, response);

            return;
        }

        try {
            var session = authenticateSession.execute(token.get());

            if (session.mustChangePassword() && !isAllowedWhenRestricted(request)) {
                problems.write(
                        response,
                        HttpStatus.FORBIDDEN,
                        "password_change_required",
                        "você precisa trocar a senha antes de continuar"
                );

                return;
            }

            request.setAttribute(ActorRequestAttribute.NAME, session.actor());

            var context = SecurityContextHolder.createEmptyContext();

            var authentication = UsernamePasswordAuthenticationToken.authenticated(
                    session.actor(),
                    null,
                    List.of()
            );

            context.setAuthentication(authentication);

            SecurityContextHolder.setContext(context);
        }
        catch (UnauthorizedException e) {
            problems.write(
                    response,
                    HttpStatus.UNAUTHORIZED,
                    "session_invalid",
                    e.getMessage()
            );

            return;
        }

        chain.doFilter(request, response);
    }

    private static Optional<String> tokenOf(HttpServletRequest request) {
        var cookies = request.getCookies();

        if (cookies == null)
            return Optional.empty();

        for (Cookie cookie : cookies)
            if (COOKIE_NAME.equals(cookie.getName()))
                return Optional.of(cookie.getValue());

        return Optional.empty();
    }

    /** Compara método e caminho sem a barra final, para {@code /users/} não escapar da restrição. */
    private static boolean isAllowedWhenRestricted(HttpServletRequest request) {
        var path = request.getRequestURI().substring(request.getContextPath().length());

        while (path.length() > 1 && path.endsWith("/"))
            path = path.substring(0, path.length() - 1);

        return RESTRICTED_SESSION_ROUTES.contains(request.getMethod() + " " + path);
    }
}
