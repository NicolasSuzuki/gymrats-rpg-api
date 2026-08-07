package com.gymrats.api.feed.domain;

import com.gymrats.api.shared.domain.AuditableEntity;
import com.gymrats.api.user.domain.User;
import com.gymrats.api.workout.domain.WorkoutSession;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "feed_posts")
public class FeedPost extends AuditableEntity {
  @Id private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "owner_id", nullable = false)
  private User owner;

  @OneToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "workout_session_id", nullable = false, unique = true)
  private WorkoutSession workoutSession;

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "media_id")
  private WorkoutMedia media;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 24)
  private FeedPostCategory category;

  @Column(nullable = false, length = 160)
  private String title;

  @Column(length = 280)
  private String caption;

  protected FeedPost() {}

  public FeedPost(
      User owner,
      WorkoutSession workoutSession,
      WorkoutMedia media,
      String title,
      String caption) {
    this.id = UUID.randomUUID();
    this.owner = owner;
    this.workoutSession = workoutSession;
    this.media = media;
    this.category = FeedPostCategory.WORKOUT;
    this.title = title;
    this.caption = caption == null || caption.isBlank() ? null : caption.trim();
  }

  public void attachMedia(WorkoutMedia media) { this.media = media; }
  public void clearMedia() { this.media = null; }

  public UUID getId() { return id; }
  public User getOwner() { return owner; }
  public WorkoutSession getWorkoutSession() { return workoutSession; }
  public WorkoutMedia getMedia() { return media; }
  public FeedPostCategory getCategory() { return category; }
  public String getTitle() { return title; }
  public String getCaption() { return caption; }
}
