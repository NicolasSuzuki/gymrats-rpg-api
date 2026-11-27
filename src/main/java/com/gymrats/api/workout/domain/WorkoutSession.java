package com.gymrats.api.workout.domain;

import com.gymrats.api.plan.domain.WorkoutPlan;
import com.gymrats.api.shared.domain.AuditableEntity;
import com.gymrats.api.user.domain.User;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "workout_sessions")
public class WorkoutSession extends AuditableEntity {
  @Id private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "owner_id", nullable = false)
  private User owner;

  @Column(name = "active_user_id", unique = true) private UUID activeUserId;
  @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private WorkoutSessionStatus status;
  @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private WorkoutSessionSource source;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "plan_version_id")
  private WorkoutPlan planVersion;

  @Column(name = "started_at", nullable = false) private Instant startedAt;
  @Column(name = "completed_at") private Instant completedAt;
  @Column(name = "xp_awarded") private Integer xpAwarded;

  @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderBy("position ASC")
  private List<WorkoutSessionExercise> exercises = new ArrayList<>();

  protected WorkoutSession() {}

  public WorkoutSession(User owner, WorkoutSessionSource source, WorkoutPlan planVersion) {
    this.id = UUID.randomUUID();
    this.owner = owner;
    this.activeUserId = owner.getId();
    this.status = WorkoutSessionStatus.IN_PROGRESS;
    this.source = source;
    this.planVersion = planVersion;
    this.startedAt = Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MICROS);
  }

  public UUID getId() { return id; }
  public User getOwner() { return owner; }
  public WorkoutSessionStatus getStatus() { return status; }
  public WorkoutSessionSource getSource() { return source; }
  public WorkoutPlan getPlanVersion() { return planVersion; }
  public Instant getStartedAt() { return startedAt; }
  public Instant getCompletedAt() { return completedAt; }
  public Integer getXpAwarded() { return xpAwarded; }
  public List<WorkoutSessionExercise> getExercises() { return List.copyOf(exercises); }
  public void addExercise(WorkoutSessionExercise exercise) { exercises.add(exercise); }

  public void complete() {
    this.status = WorkoutSessionStatus.COMPLETED;
    this.completedAt = Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MICROS);
    this.xpAwarded = 50;
    this.activeUserId = null;
  }

  public void cancel() {
    this.status = WorkoutSessionStatus.CANCELLED;
    this.completedAt = Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MICROS);
    this.xpAwarded = 0;
    this.activeUserId = null;
  }
}
