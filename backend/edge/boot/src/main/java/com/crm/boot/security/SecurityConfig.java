package com.crm.boot.security;

import com.crm.iam.application.usecases.commands.AuthenticateSession;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.DispatcherType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderNotFoundException;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

import java.util.ArrayList;
import java.util.List;

import static org.springframework.security.web.util.matcher.AntPathRequestMatcher.antMatcher;

/**
 * Cadeia de segurança do navegador (architecture.md §14): sessão por cookie {@code crm_session},
 * sem estado no servidor, com CSRF por cookie duplicado.
 *
 * <p><b>Rotas públicas:</b> só os quatro {@code POST} de entrada em {@code /auth} e o
 * {@code GET /actuator/health}; o springdoc entra só quando está ligado (perfil {@code dev}).
 * Todo o resto exige sessão. Escrita pública também exige CSRF.
 *
 * <p><b>CSRF:</b> o cookie {@code XSRF-TOKEN} é legível pelo JavaScript e o cliente devolve o
 * valor cru em {@code X-XSRF-TOKEN}. O handler sem token adiado carrega o token em toda requisição,
 * então o cookie sai até na resposta 401 de um {@code GET /auth/me}, que o cliente usa para obtê-lo
 * antes da primeira escrita. A proteção BREACH (valor mascarado) não se aplica a este formato:
 * o token só viaja em cookie e cabeçalho, nunca em corpo de página.
 *
 * <p>O formato do aplicativo ({@code Authorization: Bearer}) ainda não existe. Erro de segurança
 * sai em {@code ProblemDetail} como o resto da API.
 */
@Configuration
public class SecurityConfig {

    private static final String CSRF_COOKIE_NAME = "XSRF-TOKEN";

    private static final List<RequestMatcher> PUBLIC_ROUTES = List.of(
            antMatcher(HttpMethod.POST, "/auth/login"),
            antMatcher(HttpMethod.POST, "/auth/invitation/accept"),
            antMatcher(HttpMethod.POST, "/auth/password/forgot"),
            antMatcher(HttpMethod.POST, "/auth/password/reset"),
            antMatcher(HttpMethod.GET, "/actuator/health")
    );

    private static final List<RequestMatcher> API_DOCS_ROUTES = List.of(
            antMatcher("/v3/api-docs/**"),
            antMatcher("/swagger-ui/**"),
            antMatcher("/swagger-ui.html")
    );

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            AuthenticateSession authenticateSession,
            ObjectMapper mapper,
            Environment environment
    ) throws Exception {
        var problems = new ProblemJsonWriter(mapper);
        var publicRoutes = publicRoutes(environment);
        var csrfHandler = new CsrfTokenRequestAttributeHandler();

        // sem nome de atributo o token não é adiado: é carregado, e o cookie sai em toda resposta
        csrfHandler.setCsrfRequestAttributeName(null);

        http
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .requestCache(AbstractHttpConfigurer::disable)
                .csrf(csrf -> csrf
                        .csrfTokenRepository(csrfTokenRepository())
                        .csrfTokenRequestHandler(csrfHandler))
                .exceptionHandling(handling -> handling
                        .authenticationEntryPoint((request, response, exception) -> problems.write(
                                response,
                                HttpStatus.UNAUTHORIZED,
                                "session_invalid",
                                "sua sessão não vale mais; entre de novo"
                        ))
                        .accessDeniedHandler((request, response, exception) -> problems.write(
                                response,
                                HttpStatus.FORBIDDEN,
                                exception instanceof CsrfException ? "csrf_invalid" : "forbidden",
                                exception instanceof CsrfException
                                        ? "requisição recusada; recarregue a página e tente de novo"
                                        : "você não tem permissão para isto"
                        )))
                .authorizeHttpRequests(requests -> requests
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        .requestMatchers(publicRoutes).permitAll()
                        .anyRequest().authenticated())
                .addFilterBefore(
                        new SessionAuthenticationFilter(authenticateSession, publicRoutes, problems),
                        AnonymousAuthenticationFilter.class
                );

        return http.build();
    }

    /**
     * Impede o Spring Boot de criar o usuário padrão com senha gerada (que ele escreve no log):
     * aqui ninguém se autentica por usuário e senha do Spring Security, só pela sessão do filtro.
     */
    @Bean
    public AuthenticationManager authenticationManager() {
        return authentication -> {
            throw new ProviderNotFoundException("a autenticação é pela sessão, não por usuário e senha");
        };
    }

    private static CookieCsrfTokenRepository csrfTokenRepository() {
        var repository = CookieCsrfTokenRepository.withHttpOnlyFalse();

        repository.setCookieName(CSRF_COOKIE_NAME);
        repository.setCookieCustomizer(cookie -> cookie.sameSite("Lax"));

        return repository;
    }

    private static RequestMatcher publicRoutes(Environment environment) {
        var routes = new ArrayList<>(PUBLIC_ROUTES);

        if (environment.getProperty("springdoc.api-docs.enabled", Boolean.class, false))
            routes.addAll(API_DOCS_ROUTES);

        return new OrRequestMatcher(routes);
    }
}
