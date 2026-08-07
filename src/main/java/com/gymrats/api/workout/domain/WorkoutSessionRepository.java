package com.gymrats.api.workout.domain;

import java.util.Optional;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkoutSessionRepository extends JpaRepository<WorkoutSession, UUID> {
  Optional<WorkoutSession> findByOwnerIdAndStatus(UUID ownerId, WorkoutSessionStatus status);

  Optional<WorkoutSession> findByIdAndOwnerId(UUID id, UUID ownerId);
  List<WorkoutSession> findAllByOwnerIdAndStatus(
      UUID ownerId, WorkoutSessionStatus status);
  List<WorkoutSession> findByOwnerIdAndStatusAndCompletedAtGreaterThanEqualAndCompletedAtLessThan(
      UUID ownerId, WorkoutSessionStatus status, Instant start, Instant end);
}
