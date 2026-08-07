package com.gymrats.api.workout.api;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.math.BigDecimal;
import java.util.UUID;

public record SessionSetRequest(
    UUID id,
    @DecimalMin("0.0") BigDecimal loadKg,
    @Min(0) Integer repetitions,
    @Min(0) Integer durationSeconds,
    @Min(0) Integer distanceMeters,
    @Min(0) Integer restSeconds,
    @Min(1) @Max(10) Integer rpe) {}
