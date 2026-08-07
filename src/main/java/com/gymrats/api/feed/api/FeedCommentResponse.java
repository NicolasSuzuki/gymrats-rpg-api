package com.gymrats.api.feed.api;

import com.gymrats.api.feed.domain.FeedComment;
import java.time.Instant;
import java.util.UUID;

public record FeedCommentResponse(
    UUID id,
    UUID postId,
    UUID ownerId,
    String authorName,
    String content,
    Instant createdAt,
    boolean canDelete) {
  public static FeedCommentResponse from(FeedComment comment, UUID viewerId) {
    return new FeedCommentResponse(
        comment.getId(),
        comment.getPost().getId(),
        comment.getOwner().getId(),
        comment.getOwner().getName(),
        comment.getContent(),
        comment.getCreatedAt(),
        comment.getOwner().getId().equals(viewerId));
  }
}
