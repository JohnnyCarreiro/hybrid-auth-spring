package com.johnnycarreiro.hybridauth.auth.web;

import com.johnnycarreiro.hybridauth.auth.domain.shared.AuthErrorCode;
import com.johnnycarreiro.hybridauth.auth.domain.shared.AuthException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * The single web edge that turns domain failures into responses (RFC 7807 {@link ProblemDetail}).
 *
 * <p>An {@link AuthException} maps to the HTTP status carried by its {@link AuthErrorCode}, and the
 * stable code name is attached as a {@code code} property so clients (and tests) can branch on it
 * even when several codes share a status — e.g. the 401 family in later refresh-rotation features.
 * Keeping the mapping data-driven (status lives on the enum) means new error codes need no change
 * here. Bean-validation failures become a 400.
 */
@RestControllerAdvice
public class AuthExceptionHandler {

  @ExceptionHandler(AuthException.class)
  public ProblemDetail handleAuth(AuthException ex) {
    ProblemDetail problem =
        ProblemDetail.forStatusAndDetail(
            HttpStatusCode.valueOf(ex.code().httpStatus()), ex.getMessage());
    problem.setProperty("code", ex.code().name());
    return problem;
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ProblemDetail handleIllegalArgument(IllegalArgumentException ex) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
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
}
