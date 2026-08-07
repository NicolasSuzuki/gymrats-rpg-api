package com.gymrats.api.plan.domain;

import com.gymrats.api.user.domain.Modality;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkoutPlanRepository extends JpaRepository<WorkoutPlan, UUID> {
  @EntityGraph(attributePaths = {"exercises", "exercises.exercise"})
  Optional<WorkoutPlan> findByIdAndOwnerId(UUID id, UUID ownerId);

  @EntityGraph(attributePaths = {"exercises", "exercises.exercise"})
  List<WorkoutPlan> findByOwnerIdAndCurrentVersionTrueAndStatusOrderByUpdatedAtDesc(
      UUID ownerId, WorkoutPlanStatus status);

  @EntityGraph(attributePaths = {"exercises", "exercises.exercise"})
  List<WorkoutPlan> findByOwnerIdAndCurrentVersionTrueAndStatusAndModalityOrderByUpdatedAtDesc(
      UUID ownerId, WorkoutPlanStatus status, Modality modality);
}
