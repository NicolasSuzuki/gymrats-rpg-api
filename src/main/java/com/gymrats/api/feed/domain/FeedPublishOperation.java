package com.gymrats.api.feed.domain;

import com.gymrats.api.user.domain.User;
import com.gymrats.api.workout.domain.WorkoutSession;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
    name = "feed_publish_operations",
    uniqueConstraints = @UniqueConstraint(columnNames = {"owner_id", "idempotency_key"}))
public class FeedPublishOperation {
  @Id private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "owner_id", nullable = false)
  private User owner;

  @Column(name = "idempotency_key", nullable = false, length = 120)
  private String idempotencyKey;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "session_id", nullable = false)
  private WorkoutSession session;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "post_id", nullable = false)
  private FeedPost post;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  protected FeedPublishOperation() {}

  public FeedPublishOperation(
      User owner, String idempotencyKey, WorkoutSession session, FeedPost post) {
    this.id = UUID.randomUUID();
    this.owner = owner;
    this.idempotencyKey = idempotencyKey;
    this.session = session;
    this.post = post;
  }

  @PrePersist
  void onCreate() { this.createdAt = Instant.now(); }

  public WorkoutSession getSession() { return session; }
  public FeedPost getPost() { return post; }
}
