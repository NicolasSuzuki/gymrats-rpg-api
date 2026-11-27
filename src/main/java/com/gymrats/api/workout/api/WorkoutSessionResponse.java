package com.gymrats.api.workout.api;

import com.gymrats.api.workout.domain.SessionExerciseStatus;
import com.gymrats.api.workout.domain.WorkoutSession;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record WorkoutSessionResponse(
    UUID id,
    String status,
    String source,
    PlanSnapshotResponse planSnapshot,
    List<UUID> exerciseIds,
    List<SessionExerciseResponse> exercises,
    Instant startedAt,
    Instant completedAt,
    Integer xpAwarded,
    WorkoutSummaryResponse summary) {
  public static WorkoutSessionResponse from(WorkoutSession session) {
    return from(session, java.util.Map.of());
  }
  public static WorkoutSessionResponse from(WorkoutSession session, java.util.Map<UUID, String> names) {
    var items = session.getExercises();
    var completed = items.stream().filter(item -> item.getStatus() == SessionExerciseStatus.COMPLETED).count();
    var skipped = items.stream().filter(item -> item.getStatus() == SessionExerciseStatus.SKIPPED).count();
    var pending = items.size() - completed - skipped;
    return new WorkoutSessionResponse(
        session.getId(),
        session.getStatus().name(),
        session.getSource().name(),
        session.getPlanVersion() == null ? null : PlanSnapshotResponse.from(session.getPlanVersion()),
        items.stream().map(item -> item.getExerciseId()).toList(),
        items.stream().map(item -> SessionExerciseResponse.from(item, names.get(item.getExerciseId()))).toList(),
        session.getStartedAt(),
        session.getCompletedAt(),
        session.getXpAwarded(),
        new WorkoutSummaryResponse(items.size(), completed, skipped, pending));
  }

  public record WorkoutSummaryResponse(
      long totalExercises,
      long completedExercises,
      long skippedExercises,
      long pendingExercises) {}
}
