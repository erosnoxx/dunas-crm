package com.crm.boot.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;

import java.io.IOException;

/**
 * Escreve à mão o erro que o {@code RestExceptionHandler} escreveria: os filtros de segurança
 * rodam antes do MVC, e a exception handler dele não os alcança.
 *
 * <p>Mesmo formato do handler ({@link ProblemDetail} com {@code status}, {@code detail} e
 * {@code code}), serializado pelo {@link ObjectMapper} do projeto, nunca por concatenação.
 */
class ProblemJsonWriter {

    private final ObjectMapper mapper;

    ProblemJsonWriter(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    void write(
            HttpServletResponse response,
            HttpStatus status,
            String code,
            String detail
    ) throws IOException {
        var problem = ProblemDetail.forStatusAndDetail(status, detail);

        problem.setProperty("code", code);

        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        mapper.writeValue(response.getOutputStream(), problem);
    }
}
