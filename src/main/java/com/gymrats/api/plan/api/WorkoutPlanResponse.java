package com.gymrats.api.plan.api;

import com.gymrats.api.plan.domain.WorkoutPlan;
import java.util.List;
import java.util.UUID;

public record WorkoutPlanResponse(
    UUID id,
    String name,
    String modality,
    List<UUID> exerciseIds,
    String status,
    List<PlannedExerciseResponse> plannedParameters,
    int version,
    boolean currentVersion,
    UUID previousVersionId) {
  public static WorkoutPlanResponse from(WorkoutPlan plan) {
    return new WorkoutPlanResponse(
        plan.getId(),
        plan.getName(),
        plan.getModality().name(),
        plan.getExercises().stream().map(item -> item.getExercise().getId()).toList(),
        plan.getStatus().name(),
        plan.getExercises().stream().map(PlannedExerciseResponse::from).toList(),
        plan.getVersion(),
        plan.isCurrentVersion(),
        plan.getPreviousVersionId());
  }
}
