package com.gymrats.api.workout.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import java.util.List;

public record ExercisePerformanceDataRequest(
    @Valid @Size(max = 100) List<SessionSetRequest> sets,
    @Size(max = 1000) String notes) {}
