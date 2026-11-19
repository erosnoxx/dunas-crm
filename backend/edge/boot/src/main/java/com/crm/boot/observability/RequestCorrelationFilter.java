package com.crm.boot.observability;

import com.crm.common.infrastructure.observability.CorrelationContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Dá a cada requisição um {@code requestId} e um {@code correlationId} no MDC, para que todo log
 * dela (inclusive o do filtro de segurança, que roda depois deste) leve os dois (D39).
 *
 * <p>O {@code requestId} é sempre novo. O {@code correlationId} é o que o cliente mandou em
 * {@code X-Correlation-Id}, <b>se</b> for um id válido (letras, dígitos, ponto, sublinhado e
 * hífen, até 64), e senão é o próprio {@code requestId}: a requisição é a raiz da trilha. Valor
 * inválido é descartado em silêncio, para que ninguém injete quebra de linha ou texto longo no
 * log.
 *
 * <p>Os dois voltam em cabeçalhos da resposta, para quem reporta um problema poder citá-los.
 */
class RequestCorrelationFilter extends OncePerRequestFilter {

    static final String REQUEST_ID_HEADER = "X-Request-Id";
    static final String CORRELATION_ID_HEADER = "X-Correlation-Id";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain
    ) throws ServletException, IOException {
        var requestId = CorrelationContext.newId();
        var incoming = request.getHeader(CORRELATION_ID_HEADER);
        var correlationId = CorrelationContext.isValid(incoming) ? incoming : requestId;

        response.setHeader(REQUEST_ID_HEADER, requestId);
        response.setHeader(CORRELATION_ID_HEADER, correlationId);

        try (var ignored = CorrelationContext.open(requestId, correlationId)) {
            chain.doFilter(request, response);
        }
    }
}
