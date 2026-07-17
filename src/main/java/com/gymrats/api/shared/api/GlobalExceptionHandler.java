package com.gymrats.api.shared.api;

import com.gymrats.api.auth.application.EmailAlreadyUsedException;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
  @ExceptionHandler(EmailAlreadyUsedException.class)
  ResponseEntity<ApiError> duplicateEmail(EmailAlreadyUsedException exception, HttpServletRequest request) { return error(HttpStatus.CONFLICT, "EMAIL_ALREADY_USED", exception.getMessage(), request, List.of()); }
  @ExceptionHandler(BadCredentialsException.class)
  ResponseEntity<ApiError> badCredentials(HttpServletRequest request) { return error(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "E-mail ou senha inválidos.", request, List.of()); }
  @ExceptionHandler(MethodArgumentNotValidException.class)
  ResponseEntity<ApiError> validation(MethodArgumentNotValidException exception, HttpServletRequest request) { var fields = exception.getBindingResult().getFieldErrors().stream().map(item -> new ApiError.FieldError(item.getField(), item.getDefaultMessage())).toList(); return error(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Verifique os campos informados.", request, fields); }
  private ResponseEntity<ApiError> error(HttpStatus status, String code, String message, HttpServletRequest request, List<ApiError.FieldError> fields) { return ResponseEntity.status(status).body(new ApiError(Instant.now(), status.value(), code, message, request.getRequestURI(), fields)); }
}
