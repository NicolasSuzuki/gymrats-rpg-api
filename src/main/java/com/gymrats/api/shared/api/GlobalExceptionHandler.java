package com.gymrats.api.shared.api;

import com.gymrats.api.auth.application.EmailAlreadyUsedException;
import com.gymrats.api.shared.application.DomainConflictException;
import com.gymrats.api.shared.application.MediaStorageException;
import com.gymrats.api.shared.application.ResourceNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@RestControllerAdvice
public class GlobalExceptionHandler {
  @ExceptionHandler(EmailAlreadyUsedException.class)
  ResponseEntity<ApiError> duplicateEmail(
      EmailAlreadyUsedException exception, HttpServletRequest request) {
    return error(
        HttpStatus.CONFLICT,
        "EMAIL_ALREADY_USED",
        exception.getMessage(),
        request,
        List.of());
  }

  @ExceptionHandler({BadCredentialsException.class, DisabledException.class})
  ResponseEntity<ApiError> badCredentials(HttpServletRequest request) {
    return error(
        HttpStatus.UNAUTHORIZED,
        "INVALID_CREDENTIALS",
        "E-mail ou senha inválidos.",
        request,
        List.of());
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  ResponseEntity<ApiError> validation(
      MethodArgumentNotValidException exception, HttpServletRequest request) {
    var fields =
        exception.getBindingResult().getFieldErrors().stream()
            .map(item -> new ApiError.FieldError(item.getField(), item.getDefaultMessage()))
            .toList();
    return error(
        HttpStatus.BAD_REQUEST,
        "VALIDATION_ERROR",
        "Verifique os campos informados.",
        request,
        fields);
  }

  @ExceptionHandler(ResourceNotFoundException.class)
  ResponseEntity<ApiError> notFound(
      ResourceNotFoundException exception, HttpServletRequest request) {
    return error(
        HttpStatus.NOT_FOUND, "NOT_FOUND", exception.getMessage(), request, List.of());
  }

  @ExceptionHandler(DomainConflictException.class)
  ResponseEntity<ApiError> conflict(
      DomainConflictException exception, HttpServletRequest request) {
    return error(
        HttpStatus.CONFLICT, "CONFLICT", exception.getMessage(), request, List.of());
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  ResponseEntity<ApiError> dataConflict(
      DataIntegrityViolationException exception, HttpServletRequest request) {
    return error(
        HttpStatus.CONFLICT,
        "CONFLICT",
        "A operação conflita com o estado atual do recurso.",
        request,
        List.of());
  }

  @ExceptionHandler(AccessDeniedException.class)
  ResponseEntity<ApiError> forbidden(
      AccessDeniedException exception, HttpServletRequest request) {
    return error(
        HttpStatus.FORBIDDEN,
        "FORBIDDEN",
        "Você não tem permissão para executar esta ação.",
        request,
        List.of());
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  ResponseEntity<ApiError> malformedBody(
      HttpMessageNotReadableException exception, HttpServletRequest request) {
    return error(
        HttpStatus.BAD_REQUEST,
        "INVALID_REQUEST",
        "O corpo da solicitação é inválido.",
        request,
        List.of());
  }

  @ExceptionHandler(MaxUploadSizeExceededException.class)
  ResponseEntity<ApiError> uploadTooLarge(HttpServletRequest request) {
    return error(
        HttpStatus.PAYLOAD_TOO_LARGE,
        "FILE_TOO_LARGE",
        "A foto deve ter no máximo 5 MB.",
        request,
        List.of());
  }

  @ExceptionHandler(MediaStorageException.class)
  ResponseEntity<ApiError> mediaUnavailable(
      MediaStorageException exception, HttpServletRequest request) {
    return error(
        HttpStatus.SERVICE_UNAVAILABLE,
        "MEDIA_UNAVAILABLE",
        exception.getMessage(),
        request,
        List.of());
  }

  @ExceptionHandler(Exception.class)
  ResponseEntity<ApiError> unexpected(HttpServletRequest request) {
    return error(
        HttpStatus.INTERNAL_SERVER_ERROR,
        "INTERNAL_ERROR",
        "Não foi possível concluir a solicitação.",
        request,
        List.of());
  }

  private ResponseEntity<ApiError> error(
      HttpStatus status,
      String code,
      String message,
      HttpServletRequest request,
      List<ApiError.FieldError> fields) {
    return ResponseEntity.status(status)
        .body(
            new ApiError(
                Instant.now(), status.value(), code, message, request.getRequestURI(), fields));
  }
}
