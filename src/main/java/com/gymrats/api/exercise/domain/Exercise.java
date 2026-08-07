package com.gymrats.api.exercise.domain;

import com.gymrats.api.shared.domain.AuditableEntity;
import com.gymrats.api.user.domain.Modality;
import com.gymrats.api.user.domain.User;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "exercises")
public class Exercise extends AuditableEntity {
  @Id private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "owner_id")
  private User owner;

  @Column(nullable = false, length = 120) private String name;
  @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private Modality modality;
  @Column(nullable = false, length = 80) private String category;
  @Column(name = "muscle_group", length = 80) private String muscleGroup;
  @Column(length = 1000) private String description;
  @Column(length = 120) private String equipment;
  @Enumerated(EnumType.STRING) @Column(length = 20) private Difficulty difficulty;
  @Column(length = 80) private String terrain;
  @Column(length = 80) private String stroke;
  @Column(name = "pool_length_meters") private Integer poolLengthMeters;
  @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private ExerciseVisibility visibility;
  @Column(nullable = false) private boolean archived;

  @ElementCollection
  @CollectionTable(name = "exercise_instructions", joinColumns = @JoinColumn(name = "exercise_id"))
  @OrderColumn(name = "position")
  @Column(name = "instruction", nullable = false, length = 500)
  private List<String> instructions = new ArrayList<>();

  protected Exercise() {}

  public Exercise(User owner, ExerciseVisibility visibility) {
    this.id = UUID.randomUUID();
    this.owner = owner;
    this.visibility = visibility;
    this.archived = false;
  }

  public UUID getId() { return id; }
  public User getOwner() { return owner; }
  public String getName() { return name; }
  public Modality getModality() { return modality; }
  public String getCategory() { return category; }
  public String getMuscleGroup() { return muscleGroup; }
  public String getDescription() { return description; }
  public String getEquipment() { return equipment; }
  public Difficulty getDifficulty() { return difficulty; }
  public String getTerrain() { return terrain; }
  public String getStroke() { return stroke; }
  public Integer getPoolLengthMeters() { return poolLengthMeters; }
  public ExerciseVisibility getVisibility() { return visibility; }
  public boolean isArchived() { return archived; }
  public List<String> getInstructions() { return List.copyOf(instructions); }

  public void update(
      String name,
      Modality modality,
      String category,
      String muscleGroup,
      String description,
      List<String> instructions,
      String equipment,
      Difficulty difficulty,
      String terrain,
      String stroke,
      Integer poolLengthMeters) {
    this.name = name.trim();
    this.modality = modality;
    this.category = category.trim();
    this.muscleGroup = normalize(muscleGroup);
    this.description = normalize(description);
    this.instructions = instructions == null ? new ArrayList<>() : new ArrayList<>(instructions);
    this.equipment = normalize(equipment);
    this.difficulty = difficulty;
    this.terrain = normalize(terrain);
    this.stroke = normalize(stroke);
    this.poolLengthMeters = poolLengthMeters;
  }

  public void archive() { this.archived = true; }

  private String normalize(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }
}
