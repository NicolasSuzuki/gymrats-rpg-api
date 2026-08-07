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
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "advanced_rank_profiles")
public class AdvancedRankProfile extends AuditableEntity {
  @Id
  @Column(name = "user_id")
  private UUID userId;

  @MapsId
  @OneToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id")
  private User user;

  @Enumerated(EnumType.STRING)
  @Column(name = "current_rank", nullable = false, length = 16)
  private AdvancedRank currentRank;

  @Column(name = "s1_since")
  private Instant s1Since;

  @Column(name = "national_since")
  private Instant nationalSince;

  @Column(name = "pause_started_at")
  private Instant pauseStartedAt;

  @Column(name = "pause_reason", length = 160)
  private String pauseReason;

  @Column(name = "s1_paused_seconds", nullable = false)
  private long s1PausedSeconds;

  @Column(name = "protected_until")
  private Instant protectedUntil;

  protected AdvancedRankProfile() {}

  public AdvancedRankProfile(User user) {
    this.user = user;
    this.currentRank = AdvancedRank.A;
  }

  public AdvancedRank getCurrentRank() { return currentRank; }
  public User getUser() { return user; }
  public Instant getS1Since() { return s1Since; }
  public Instant getNationalSince() { return nationalSince; }
  public Instant getPauseStartedAt() { return pauseStartedAt; }
  public String getPauseReason() { return pauseReason; }
  public long getS1PausedSeconds() { return s1PausedSeconds; }
  public Instant getProtectedUntil() { return protectedUntil; }
  public boolean isPaused() { return pauseStartedAt != null; }

  public void transition(AdvancedRank next, Instant now, Instant protectionEnd) {
    this.currentRank = next;
    this.protectedUntil = protectionEnd;
    if (next == AdvancedRank.S1 && s1Since == null) {
      this.s1Since = now;
      this.s1PausedSeconds = 0;
    } else if (next.ordinal() < AdvancedRank.S1.ordinal()) {
      this.s1Since = null;
      this.s1PausedSeconds = 0;
    }
    if (next == AdvancedRank.NATIONAL) {
      this.nationalSince = now;
    } else if (this.nationalSince != null) {
      this.nationalSince = null;
    }
  }

  public void pause(String reason, Instant now) {
    if (pauseStartedAt == null) {
      pauseStartedAt = now;
      pauseReason = reason;
    }
  }

  public void resume(Instant now) {
    if (pauseStartedAt != null) {
      if (s1Since != null) {
        s1PausedSeconds += Math.max(0, Duration.between(pauseStartedAt, now).getSeconds());
      }
      pauseStartedAt = null;
      pauseReason = null;
    }
  }
}
