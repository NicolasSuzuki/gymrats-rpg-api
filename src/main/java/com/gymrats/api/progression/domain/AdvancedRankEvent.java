package com.gymrats.api.progression.domain;

import com.gymrats.api.shared.domain.AuditableEntity;
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
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "advanced_rank_events")
public class AdvancedRankEvent extends AuditableEntity {
  @Id private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Enumerated(EnumType.STRING)
  @Column(name = "previous_rank", nullable = false, length = 16)
  private AdvancedRank previousRank;

  @Enumerated(EnumType.STRING)
  @Column(name = "new_rank", nullable = false, length = 16)
  private AdvancedRank newRank;

  @Column(nullable = false, length = 160)
  private String reason;

  @Column(name = "occurred_at", nullable = false)
  private Instant occurredAt;

  protected AdvancedRankEvent() {}

  public AdvancedRankEvent(
      User user,
      AdvancedRank previousRank,
      AdvancedRank newRank,
      String reason,
      Instant occurredAt) {
    this.id = UUID.randomUUID();
    this.user = user;
    this.previousRank = previousRank;
    this.newRank = newRank;
    this.reason = reason;
    this.occurredAt = occurredAt;
  }

  public UUID getId() { return id; }
  public AdvancedRank getPreviousRank() { return previousRank; }
  public AdvancedRank getNewRank() { return newRank; }
  public String getReason() { return reason; }
  public Instant getOccurredAt() { return occurredAt; }
}
