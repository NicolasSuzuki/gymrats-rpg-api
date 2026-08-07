package com.gymrats.api.progression.domain;

import com.gymrats.api.shared.domain.AuditableEntity;
import com.gymrats.api.user.domain.CharacterClass;
import com.gymrats.api.user.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
    name = "class_mastery_events",
    uniqueConstraints =
        @UniqueConstraint(columnNames = {"user_id", "character_class", "source_id"}))
public class ClassMasteryEvent extends AuditableEntity {
  @Id private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Enumerated(EnumType.STRING)
  @Column(name = "character_class", nullable = false, length = 20)
  private CharacterClass characterClass;

  @Column(name = "source_id", nullable = false)
  private UUID sourceId;

  @Column(nullable = false)
  private int amount;

  @Column(name = "occurred_at", nullable = false)
  private Instant occurredAt;

  protected ClassMasteryEvent() {}

  public ClassMasteryEvent(
      User user,
      CharacterClass characterClass,
      UUID sourceId,
      int amount,
      Instant occurredAt) {
    this.id = UUID.randomUUID();
    this.user = user;
    this.characterClass = characterClass;
    this.sourceId = sourceId;
    this.amount = amount;
    this.occurredAt = occurredAt;
  }

  public UUID getId() { return id; }
  public CharacterClass getCharacterClass() { return characterClass; }
  public int getAmount() { return amount; }
  public Instant getOccurredAt() { return occurredAt; }
}
