package com.gymrats.api.plan.domain;

import com.gymrats.api.shared.domain.AuditableEntity;
import com.gymrats.api.user.domain.Modality;
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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "workout_plans")
public class WorkoutPlan extends AuditableEntity {
  @Id private UUID id;
  @Column(name = "root_id", nullable = false) private UUID rootId;
  @Column(name = "current_root_id", unique = true) private UUID currentRootId;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "owner_id", nullable = false)
  private User owner;

  @Column(nullable = false, length = 120) private String name;
  @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private Modality modality;
  @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private WorkoutPlanStatus status;
  @Column(nullable = false) private int version;
  @Column(name = "current_version", nullable = false) private boolean currentVersion;
  @Column(name = "previous_version_id") private UUID previousVersionId;

  @OneToMany(mappedBy = "plan", cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderBy("position ASC")
  private List<WorkoutPlanExercise> exercises = new ArrayList<>();

  protected WorkoutPlan() {}

  public WorkoutPlan(User owner, String name, Modality modality) {
    this.id = UUID.randomUUID();
    this.rootId = this.id;
    this.currentRootId = this.rootId;
    this.owner = owner;
    this.name = name.trim();
    this.modality = modality;
    this.status = WorkoutPlanStatus.ACTIVE;
    this.version = 1;
    this.currentVersion = true;
  }

  public WorkoutPlan(WorkoutPlan previous, String name, Modality modality) {
    this.id = UUID.randomUUID();
    this.rootId = previous.rootId;
    this.currentRootId = this.rootId;
    this.owner = previous.owner;
    this.name = name.trim();
    this.modality = modality;
    this.status = previous.status;
    this.version = previous.version + 1;
    this.currentVersion = true;
    this.previousVersionId = previous.id;
  }

  public UUID getId() { return id; }
  public UUID getRootId() { return rootId; }
  public User getOwner() { return owner; }
  public String getName() { return name; }
  public Modality getModality() { return modality; }
  public WorkoutPlanStatus getStatus() { return status; }
  public int getVersion() { return version; }
  public boolean isCurrentVersion() { return currentVersion; }
  public UUID getPreviousVersionId() { return previousVersionId; }
  public List<WorkoutPlanExercise> getExercises() { return List.copyOf(exercises); }
  public void addExercise(WorkoutPlanExercise exercise) { exercises.add(exercise); }
  public void supersede() { this.currentVersion = false; this.currentRootId = null; }
  public void archive() { this.status = WorkoutPlanStatus.ARCHIVED; }
  public void restore() { this.status = WorkoutPlanStatus.ACTIVE; }
}
