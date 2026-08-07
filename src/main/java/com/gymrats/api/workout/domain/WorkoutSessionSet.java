package com.gymrats.api.workout.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "workout_session_sets")
public class WorkoutSessionSet {
  @Id private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "session_exercise_id", nullable = false)
  private WorkoutSessionExercise sessionExercise;

  @Column(nullable = false) private int position;
  @Column(name = "load_kg", precision = 8, scale = 2) private BigDecimal loadKg;
  private Integer repetitions;
  @Column(name = "duration_seconds") private Integer durationSeconds;
  @Column(name = "distance_meters") private Integer distanceMeters;
  @Column(name = "rest_seconds") private Integer restSeconds;
  private Integer rpe;

  protected WorkoutSessionSet() {}

  public WorkoutSessionSet(
      WorkoutSessionExercise sessionExercise,
      int position,
      BigDecimal loadKg,
      Integer repetitions,
      Integer durationSeconds,
      Integer distanceMeters,
      Integer restSeconds,
      Integer rpe) {
    this.id = UUID.randomUUID();
    this.sessionExercise = sessionExercise;
    this.position = position;
    this.loadKg = loadKg;
    this.repetitions = repetitions;
    this.durationSeconds = durationSeconds;
    this.distanceMeters = distanceMeters;
    this.restSeconds = restSeconds;
    this.rpe = rpe;
  }

  public UUID getId() { return id; }
  public BigDecimal getLoadKg() { return loadKg; }
  public Integer getRepetitions() { return repetitions; }
  public Integer getDurationSeconds() { return durationSeconds; }
  public Integer getDistanceMeters() { return distanceMeters; }
  public Integer getRestSeconds() { return restSeconds; }
  public Integer getRpe() { return rpe; }
}
