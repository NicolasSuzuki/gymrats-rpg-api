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
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "monarch_reigns")
public class MonarchReign extends AuditableEntity {
  @Id private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Column(name = "started_at", nullable = false)
  private Instant startedAt;

  @Column(name = "ended_at")
  private Instant endedAt;

  @Column(name = "active_slot", unique = true)
  private Short activeSlot;

  protected MonarchReign() {}

  public MonarchReign(User user, Instant startedAt) {
    this.id = UUID.randomUUID();
    this.user = user;
    this.startedAt = startedAt;
    this.activeSlot = 1;
  }

  public UUID getId() {
    return id;
  }

  public User getUser() {
    return user;
  }

  public Instant getStartedAt() {
    return startedAt;
  }

  public Instant getEndedAt() {
    return endedAt;
  }

  public boolean isActive() {
    return endedAt == null;
  }

  public void end(Instant endedAt) {
    if (this.endedAt == null) {
      this.endedAt = endedAt.isBefore(startedAt) ? startedAt : endedAt;
      this.activeSlot = null;
    }
  }
}
