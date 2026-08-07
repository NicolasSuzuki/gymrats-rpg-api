package com.gymrats.api.workout.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "workout_session_exercises")
public class WorkoutSessionExercise {
  @Id private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "session_id", nullable = false)
  private WorkoutSession session;

  @Column(name = "exercise_id", nullable = false) private UUID exerciseId;
  @Column(nullable = false) private int position;
  @jakarta.persistence.Enumerated(jakarta.persistence.EnumType.STRING)
  @Column(nullable = false, length = 20)
  private SessionExerciseStatus status;
  @Column(length = 1000) private String notes;

  @OneToMany(mappedBy = "sessionExercise", cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderBy("position ASC")
  private List<WorkoutSessionSet> sets = new ArrayList<>();

  protected WorkoutSessionExercise() {}

  public WorkoutSessionExercise(WorkoutSession session, UUID exerciseId, int position) {
    this.id = UUID.randomUUID();
    this.session = session;
    this.exerciseId = exerciseId;
    this.position = position;
    this.status = SessionExerciseStatus.PENDING;
  }

  public UUID getId() { return id; }
  public UUID getExerciseId() { return exerciseId; }
  public int getPosition() { return position; }
  public SessionExerciseStatus getStatus() { return status; }
  public String getNotes() { return notes; }
  public List<WorkoutSessionSet> getSets() { return List.copyOf(sets); }
  public void updateStatus(SessionExerciseStatus status) { if (status != null) this.status = status; }

  public void replaceData(String notes, List<WorkoutSessionSet> sets) {
    this.notes = notes == null || notes.isBlank() ? null : notes.trim();
    this.sets.clear();
    this.sets.addAll(sets);
  }
}
