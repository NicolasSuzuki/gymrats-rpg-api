package com.gymrats.api.feed.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeedPostRepository extends JpaRepository<FeedPost, UUID> {
  Page<FeedPost> findAllByCategory(FeedPostCategory category, Pageable pageable);
  Optional<FeedPost> findByWorkoutSessionId(UUID sessionId);
  Optional<FeedPost> findByMediaId(UUID mediaId);
  List<FeedPost> findByOwnerId(UUID ownerId);
  List<FeedPost> findByOwnerIdAndCategoryOrderByCreatedAtDesc(
      UUID ownerId, FeedPostCategory category);
}
