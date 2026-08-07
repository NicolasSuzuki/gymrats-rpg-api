package com.gymrats.api.plan.api;

import com.gymrats.api.plan.domain.WorkoutPlanExercise;
import java.util.UUID;

public record PlannedExerciseResponse(
    UUID exerciseId,
    Integer sets,
    Integer repetitions,
    Integer durationMinutes,
    Integer distanceMeters) {
  public static PlannedExerciseResponse from(WorkoutPlanExercise item) {
    return new PlannedExerciseResponse(
        item.getExercise().getId(),
        item.getSets(),
        item.getRepetitions(),
        item.getDurationMinutes(),
        item.getDistanceMeters());
  }
}
