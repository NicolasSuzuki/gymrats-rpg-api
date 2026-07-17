package com.gymrats.api.shared.api;

import java.time.Instant;
import java.util.List;

public record ApiError(Instant timestamp, int status, String code, String message, String path, List<FieldError> fields) {
  public record FieldError(String field, String message) {}
}
