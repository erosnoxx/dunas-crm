package com.crm.web.middlewares;

import com.crm.common.application.exceptions.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import java.util.List;
import java.util.Optional;

/**
 * Tradução única de exception para {@link ProblemDetail}.
 *
 * <p>Só conhece tipos de application: o aspect de {@code @UseCase} garante que nenhuma
 * exception de domínio chega até aqui. O código de erro (architecture.md §7.3) vai na
 * propriedade {@code code}; os campos inválidos, em {@code invalidParams}. Todo erro passa por
 * {@link #respond}, então o formato é um só, venha de onde vier.
 *
 * <p>Exceptions do próprio Spring MVC (método não suportado, tipo de mídia) seguem o tratamento
 * padrão de {@link ResponseEntityExceptionHandler}.
 */
@Slf4j @RestControllerAdvice
public class RestExceptionHandler extends ResponseEntityExceptionHandler {

    static final String CODE = "code";
    static final String INVALID_PARAMS = "invalidParams";
    static final String UNEXPECTED_MESSAGE = "erro interno; tente de novo em instantes";

    @ExceptionHandler(ApplicationException.class)
    public ResponseEntity<Object> handleApplication(ApplicationException exception) {
        var status = statusOf(exception);

        logByStatus(status, exception);

        return respond(
                status,
                exception.getMessage(),
                exception.getCode(),
                invalidParamsOf(exception)
        );
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request
    ) {
        var invalidParams = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> new InvalidParam(error.getField(), error.getDefaultMessage()))
                .toList();

        return respond(
                HttpStatus.BAD_REQUEST,
                "dados inválidos",
                Optional.empty(),
                invalidParams
        );
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException exception,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request
    ) {
        return respond(
                HttpStatus.BAD_REQUEST,
                "corpo da requisição inválido",
                Optional.empty(),
                List.of()
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleUnexpected(Exception exception) {
        log.error("erro inesperado ao atender a requisição: {}", exception.getMessage(), exception);

        return respond(
                HttpStatus.INTERNAL_SERVER_ERROR,
                UNEXPECTED_MESSAGE,
                Optional.empty(),
                List.of()
        );
    }

    private static ResponseEntity<Object> respond(
            HttpStatus status,
            String message,
            Optional<String> code,
            List<InvalidParam> invalidParams
    ) {
        var problem = ProblemDetail.forStatusAndDetail(status, message);

        code.ifPresent(value -> problem.setProperty(CODE, value));

        if (!invalidParams.isEmpty())
            problem.setProperty(INVALID_PARAMS, invalidParams);

        return ResponseEntity.status(status).body(problem);
    }

    private static List<InvalidParam> invalidParamsOf(ApplicationException exception) {
        if (exception instanceof BadRequestException badRequest)
            return badRequest.getInvalidParams();

        return List.of();
    }

    private static HttpStatus statusOf(ApplicationException exception) {
        return switch (exception) {
            case BadRequestException e -> HttpStatus.BAD_REQUEST;
            case UnauthorizedException e -> HttpStatus.UNAUTHORIZED;
            case ForbiddenException e -> HttpStatus.FORBIDDEN;
            case NotFoundException e -> HttpStatus.NOT_FOUND;
            case ConflictException e -> HttpStatus.CONFLICT;
            case UnprocessableEntityException e -> HttpStatus.UNPROCESSABLE_ENTITY;
            case RateLimitExceededException e -> HttpStatus.TOO_MANY_REQUESTS;
            case ExternalServiceException e -> HttpStatus.SERVICE_UNAVAILABLE;
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }

    private static void logByStatus(HttpStatus status, ApplicationException exception) {
        if (status.is5xxServerError()) {
            log.error("erro {} ao atender a requisição: {}", status.value(), exception.getMessage(), exception);
            return;
        }

        log.debug("erro {} ao atender a requisição: {}", status.value(), exception.getMessage());
    }
}
