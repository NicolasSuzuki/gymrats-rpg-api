package com.gymrats.api.workout.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "workout_idempotency_operations")
public class IdempotencyOperation {
  @Id private UUID id;
  @Column(name = "owner_id", nullable = false) private UUID ownerId;
  @Column(name = "idempotency_key", nullable = false, length = 120) private String key;
  @Column(nullable = false, length = 20) private String operation;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "session_id", nullable = false)
  private WorkoutSession session;

  @Column(name = "created_at", nullable = false) private Instant createdAt;

  protected IdempotencyOperation() {}

  public IdempotencyOperation(UUID ownerId, String key, String operation, WorkoutSession session) {
    this.id = UUID.randomUUID();
    this.ownerId = ownerId;
    this.key = key;
    this.operation = operation;
    this.session = session;
    this.createdAt = Instant.now();
  }

  public String getOperation() { return operation; }
  public WorkoutSession getSession() { return session; }
}
