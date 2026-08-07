package com.gymrats.api.plan.api;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record PlannedExerciseRequest(
    @NotNull UUID exerciseId,
    @Min(1) Integer sets,
    @Min(1) Integer repetitions,
    @Min(1) Integer durationMinutes,
    @Min(1) Integer distanceMeters) {}
