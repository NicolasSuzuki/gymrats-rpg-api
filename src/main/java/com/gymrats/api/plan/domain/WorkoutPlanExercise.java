package com.gymrats.api.plan.domain;

import com.gymrats.api.exercise.domain.Exercise;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "workout_plan_exercises")
public class WorkoutPlanExercise {
  @Id private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "plan_id", nullable = false)
  private WorkoutPlan plan;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "exercise_id", nullable = false)
  private Exercise exercise;

  @Column(nullable = false) private int position;
  private Integer sets;
  private Integer repetitions;
  @Column(name = "duration_minutes") private Integer durationMinutes;
  @Column(name = "distance_meters") private Integer distanceMeters;

  protected WorkoutPlanExercise() {}

  public WorkoutPlanExercise(
      WorkoutPlan plan,
      Exercise exercise,
      int position,
      Integer sets,
      Integer repetitions,
      Integer durationMinutes,
      Integer distanceMeters) {
    this.id = UUID.randomUUID();
    this.plan = plan;
    this.exercise = exercise;
    this.position = position;
    this.sets = sets;
    this.repetitions = repetitions;
    this.durationMinutes = durationMinutes;
    this.distanceMeters = distanceMeters;
  }

  public Exercise getExercise() { return exercise; }
  public int getPosition() { return position; }
  public Integer getSets() { return sets; }
  public Integer getRepetitions() { return repetitions; }
  public Integer getDurationMinutes() { return durationMinutes; }
  public Integer getDistanceMeters() { return distanceMeters; }
}
