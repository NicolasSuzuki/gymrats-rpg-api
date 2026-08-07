package com.gymrats.api.user.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.Locale;

public enum Universe {
  HUNTER,
  FITNESS,
  SPOTLIGHT,
  VIGIL;

  @JsonCreator
  public static Universe from(String value) {
    return value == null ? null : valueOf(value.trim().toUpperCase(Locale.ROOT));
  }
}
