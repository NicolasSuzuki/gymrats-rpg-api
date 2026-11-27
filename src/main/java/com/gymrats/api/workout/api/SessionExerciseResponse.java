package com.gymrats.api.workout.api;

import com.gymrats.api.workout.domain.WorkoutSessionExercise;
import java.util.List;
import java.util.UUID;

public record SessionExerciseResponse(
    UUID exerciseId, String status, ExerciseDataResponse data, String exerciseName) {
  public static SessionExerciseResponse from(WorkoutSessionExercise exercise) {
    return from(exercise, null);
  }
  public static SessionExerciseResponse from(WorkoutSessionExercise exercise, String name) {
    return new SessionExerciseResponse(
        exercise.getExerciseId(),
        exercise.getStatus().name(),
        new ExerciseDataResponse(
            exercise.getSets().stream().map(SessionSetResponse::from).toList(),
            exercise.getNotes()), name);
  }

  public record ExerciseDataResponse(List<SessionSetResponse> sets, String notes) {}
}
