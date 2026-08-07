package com.gymrats.api.exercise.api;

import com.gymrats.api.exercise.domain.Exercise;
import java.util.List;
import java.util.UUID;

public record ExerciseResponse(
    UUID id,
    String name,
    String modality,
    String category,
    String muscleGroup,
    String description,
    List<String> instructions,
    String equipment,
    String difficulty,
    String terrain,
    String stroke,
    Integer poolLengthMeters,
    String visibility) {
  public static ExerciseResponse from(Exercise exercise) {
    return new ExerciseResponse(
        exercise.getId(),
        exercise.getName(),
        exercise.getModality().name(),
        exercise.getCategory(),
        exercise.getMuscleGroup(),
        exercise.getDescription(),
        exercise.getInstructions(),
        exercise.getEquipment(),
        exercise.getDifficulty() == null ? null : exercise.getDifficulty().name(),
        exercise.getTerrain(),
        exercise.getStroke(),
        exercise.getPoolLengthMeters(),
        exercise.getVisibility().name());
  }
}
