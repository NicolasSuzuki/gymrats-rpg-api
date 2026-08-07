package com.gymrats.api.plan.api;

import com.gymrats.api.user.domain.Modality;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

public record WorkoutPlanRequest(
    @NotBlank @Size(max = 120) String name,
    @NotNull Modality modality,
    @NotEmpty @Size(max = 100) List<@NotNull UUID> exerciseIds,
    @Valid List<PlannedExerciseRequest> plannedParameters) {}
