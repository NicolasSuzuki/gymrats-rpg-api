package com.gymrats.api.workout.api;

import com.gymrats.api.workout.domain.SessionExerciseStatus;
import jakarta.validation.Valid;

public record UpdateSessionExerciseRequest(
    SessionExerciseStatus status, @Valid ExercisePerformanceDataRequest data) {}
