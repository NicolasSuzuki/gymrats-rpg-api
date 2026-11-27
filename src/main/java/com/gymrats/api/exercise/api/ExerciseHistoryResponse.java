package com.gymrats.api.exercise.api;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ExerciseHistoryResponse(
    UUID exerciseId, String exerciseName, List<Entry> items, Summary summary) {
  public record Entry(UUID sessionId, Instant completedAt, List<SetEntry> sets) {}
  public record SetEntry(UUID id, BigDecimal loadKg, Integer repetitions,
      Integer durationSeconds, Integer distanceMeters, Integer restSeconds, Integer rpe) {}
  public record Summary(long sessionCount, long totalSets, BigDecimal totalVolumeKg,
      BigDecimal maxLoadKg, Integer maxRepetitions, Integer maxDistanceMeters,
      Integer maxDurationSeconds) {}
}
