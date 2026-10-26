package com.weg.benefits.common.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import com.weg.benefits.identity.IdentityException;
import com.weg.benefits.catalog.CatalogException;
import com.weg.benefits.engagement.EngagementException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.dao.DataIntegrityViolationException;

@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(IdentityException.class)
    ResponseEntity<ProblemDetail> handleIdentity(IdentityException exception) {
        return problem(exception.getStatus(), exception.getTitle(), exception.getMessage());
    }

    @ExceptionHandler(CatalogException.class)
    ResponseEntity<ProblemDetail> handleCatalog(CatalogException exception) {
        return problem(exception.getStatus(), exception.getTitle(), exception.getMessage());
    }

    @ExceptionHandler(EngagementException.class)
    ResponseEntity<ProblemDetail> handleEngagement(EngagementException exception) {
        return problem(exception.getStatus(), exception.getTitle(), exception.getMessage());
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, HttpMessageNotReadableException.class})
    ResponseEntity<ProblemDetail> handleInvalidRequest(Exception exception) {
        return problem(HttpStatus.BAD_REQUEST, "Requisição inválida", "Confira os dados enviados.");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ResponseEntity<ProblemDetail> handleInvalidPathOrParameter(MethodArgumentTypeMismatchException exception) {
        return problem(HttpStatus.BAD_REQUEST, "Parâmetro inválido", "Confira os parâmetros enviados.");
    }

    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<ProblemDetail> handleNotFound(NoResourceFoundException exception) {
        return problem(HttpStatus.NOT_FOUND, "Recurso não encontrado", "O recurso solicitado não existe.");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    ResponseEntity<ProblemDetail> handleMethodNotAllowed(HttpRequestMethodNotSupportedException exception) {
        return problem(HttpStatus.METHOD_NOT_ALLOWED, "Método não permitido",
                "O método HTTP não é permitido para este recurso.");
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    ResponseEntity<ProblemDetail> handleUnsupportedMediaType(HttpMediaTypeNotSupportedException exception) {
        return problem(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Tipo de conteúdo não suportado",
                "O tipo de conteúdo enviado não é aceito.");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ProblemDetail> handleDataConflict(DataIntegrityViolationException exception) {
        return problem(HttpStatus.CONFLICT, "Conflito", "A operação conflita com um registro ou relação existente.");
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ProblemDetail> handleUnexpectedError(Exception exception) {
        String requestId = MDC.get("requestId");
        logger.error("Unhandled request error requestId={} exceptionType={}", requestId,
                exception.getClass().getSimpleName());
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno", "Não foi possível processar a solicitação.");
    }

    private ResponseEntity<ProblemDetail> problem(HttpStatus status, String title, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        problem.setProperty("requestId", MDC.get("requestId"));
        return ResponseEntity.status(status).body(problem);
    }
}
