package com.gymrats.api.workout.api;

import com.gymrats.api.workout.domain.WorkoutSessionSet;
import java.math.BigDecimal;
import java.util.UUID;

public record SessionSetResponse(
    UUID id,
    BigDecimal loadKg,
    Integer repetitions,
    Integer durationSeconds,
    Integer distanceMeters,
    Integer restSeconds,
    Integer rpe) {
  public static SessionSetResponse from(WorkoutSessionSet set) {
    return new SessionSetResponse(
        set.getId(),
        set.getLoadKg(),
        set.getRepetitions(),
        set.getDurationSeconds(),
        set.getDistanceMeters(),
        set.getRestSeconds(),
        set.getRpe());
  }
}
