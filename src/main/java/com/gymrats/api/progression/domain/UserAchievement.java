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
    name = "user_achievements",
    uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "code"}))
public class UserAchievement extends AuditableEntity {
  @Id private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Column(nullable = false, length = 40)
  private String code;

  @Column(name = "unlocked_at", nullable = false)
  private Instant unlockedAt;

  protected UserAchievement() {}

  public UserAchievement(User user, String code, Instant unlockedAt) {
    this.id = UUID.randomUUID();
    this.user = user;
    this.code = code;
    this.unlockedAt = unlockedAt;
  }

  public UUID getId() { return id; }
  public User getUser() { return user; }
  public String getCode() { return code; }
  public Instant getUnlockedAt() { return unlockedAt; }
}
