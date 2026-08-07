package com.gymrats.api.feed.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.gymrats.api.feed.domain.FeedPost;
import com.gymrats.api.workout.domain.SessionExerciseStatus;
import java.time.Instant;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record FeedPostResponse(
    UUID id,
    UUID ownerId,
    String authorName,
    String category,
    UUID workoutSessionId,
    String title,
    String caption,
    String visual,
    String photoUrl,
    WorkoutSummary summary,
    Integer xpAwarded,
    Instant createdAt,
    long commentsCount,
    UUID groupId) {
  public static FeedPostResponse from(FeedPost post, long commentsCount) {
    var session = post.getWorkoutSession();
    var exercises = session.getExercises();
    long completed = exercises.stream()
        .filter(item -> item.getStatus() == SessionExerciseStatus.COMPLETED)
        .count();
    long skipped = exercises.stream()
        .filter(item -> item.getStatus() == SessionExerciseStatus.SKIPPED)
        .count();
    return new FeedPostResponse(
        post.getId(),
        post.getOwner().getId(),
        post.getOwner().getName(),
        post.getCategory().name(),
        session.getId(),
        post.getTitle(),
        post.getCaption(),
        post.getMedia() == null ? "FALLBACK_CARD" : "PHOTO",
        post.getMedia() == null ? null : "/api/v1/media/" + post.getMedia().getId(),
        new WorkoutSummary(exercises.size(), completed, skipped, exercises.size() - completed - skipped),
        session.getXpAwarded(),
        post.getCreatedAt(),
        commentsCount,
        null);
  }

  public record WorkoutSummary(
      long totalExercises,
      long completedExercises,
      long skippedExercises,
      long pendingExercises) {}
}
