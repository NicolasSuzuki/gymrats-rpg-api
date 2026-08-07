package com.gymrats.api.workout.api;

import com.gymrats.api.plan.api.PlannedExerciseResponse;
import com.gymrats.api.plan.domain.WorkoutPlan;
import java.util.List;
import java.util.UUID;

public record PlanSnapshotResponse(
    UUID id,
    String name,
    String modality,
    List<UUID> exerciseIds,
    List<PlannedExerciseResponse> plannedParameters,
    int version) {
  public static PlanSnapshotResponse from(WorkoutPlan plan) {
    return new PlanSnapshotResponse(
        plan.getId(),
        plan.getName(),
        plan.getModality().name(),
        plan.getExercises().stream().map(item -> item.getExercise().getId()).toList(),
        plan.getExercises().stream().map(PlannedExerciseResponse::from).toList(),
        plan.getVersion());
  }
}
