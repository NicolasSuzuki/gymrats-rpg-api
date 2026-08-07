package com.gymrats.api.user.domain;

import com.gymrats.api.shared.domain.AuditableEntity;
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
@Table(name = "weekly_goal_versions")
public class WeeklyGoalVersion extends AuditableEntity {
  @Id private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Column(nullable = false) private int target;
  @Column(name = "valid_from", nullable = false) private Instant validFrom;
  @Column(name = "valid_until") private Instant validUntil;
  @Column(name = "current_user_id", unique = true) private UUID currentUserId;

  protected WeeklyGoalVersion() {}

  public WeeklyGoalVersion(User user, int target, Instant validFrom) {
    this.id = UUID.randomUUID();
    this.user = user;
    this.target = target;
    this.validFrom = validFrom;
    this.currentUserId = user.getId();
  }

  public int getTarget() { return target; }
  public Instant getValidFrom() { return validFrom; }
  public Instant getValidUntil() { return validUntil; }
  public void endAt(Instant instant) { this.validUntil = instant; this.currentUserId = null; }
}
