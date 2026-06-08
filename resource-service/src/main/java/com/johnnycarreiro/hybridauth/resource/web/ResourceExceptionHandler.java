package com.johnnycarreiro.hybridauth.resource.web;

import com.johnnycarreiro.hybridauth.resource.domain.shared.ResourceErrorCode;
import com.johnnycarreiro.hybridauth.resource.domain.shared.ResourceException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * The single web edge that turns resource-domain failures into responses (RFC 7807 {@link
 * ProblemDetail}) — the resource-service analogue of the auth-service's {@code
 * AuthExceptionHandler}.
 *
 * <p>A {@link ResourceException} maps to the HTTP status carried by its {@link ResourceErrorCode},
 * with the stable code name attached as a {@code code} property so clients/tests can branch on it.
 * Bean-validation failures and unparseable bodies (e.g. an unknown {@code TaskStatus}) become a
 * 400. Authentication failures never arrive here — Spring Security's resource-server filter answers
 * them as 401 before the dispatch.
 */
@RestControllerAdvice
public class ResourceExceptionHandler {

  @ExceptionHandler(ResourceException.class)
  public ProblemDetail handleResource(ResourceException ex) {
    ProblemDetail problem =
        ProblemDetail.forStatusAndDetail(
            HttpStatusCode.valueOf(ex.code().httpStatus()), ex.getMessage());
    problem.setProperty("code", ex.code().name());
    return problem;
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
    String detail =
        ex.getBindingResult().getFieldErrors().stream()
            .map(fieldError -> fieldError.getField() + " " + fieldError.getDefaultMessage())
            .reduce((a, b) -> a + "; " + b)
            .orElse("invalid request");
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
    problem.setProperty("code", "VALIDATION_FAILED");
    return problem;
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ProblemDetail handleUnreadable(HttpMessageNotReadableException ex) {
    ProblemDetail problem =
        ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_REQUEST, "malformed or unreadable request body");
    problem.setProperty("code", "VALIDATION_FAILED");
    return problem;
  }
}
