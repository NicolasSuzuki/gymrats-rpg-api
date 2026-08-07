package com.gymrats.api.progression.domain;

import com.gymrats.api.shared.domain.AuditableEntity;
import com.gymrats.api.user.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
    name = "xp_events",
    uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "source_type", "source_id"}))
public class XpEvent extends AuditableEntity {
  @Id private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Column(name = "source_type", nullable = false, length = 24)
  private String sourceType;

  @Column(name = "source_id", nullable = false)
  private UUID sourceId;

  @Column(nullable = false)
  private int amount;

  @Column(name = "occurred_at", nullable = false)
  private Instant occurredAt;

  protected XpEvent() {}

  public XpEvent(User user, String sourceType, UUID sourceId, int amount, Instant occurredAt) {
    this.id = UUID.randomUUID();
    this.user = user;
    this.sourceType = sourceType;
    this.sourceId = sourceId;
    this.amount = amount;
    this.occurredAt = occurredAt;
  }

  public UUID getId() { return id; }
  public User getUser() { return user; }
  public String getSourceType() { return sourceType; }
  public UUID getSourceId() { return sourceId; }
  public int getAmount() { return amount; }
  public Instant getOccurredAt() { return occurredAt; }
}
