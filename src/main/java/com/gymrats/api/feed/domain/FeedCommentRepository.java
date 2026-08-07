package com.gymrats.api.feed.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeedCommentRepository extends JpaRepository<FeedComment, UUID> {
  List<FeedComment> findByPostIdOrderByCreatedAtAsc(UUID postId);
  Optional<FeedComment> findByIdAndPostId(UUID id, UUID postId);
  long countByPostId(UUID postId);
  List<FeedComment> findByOwnerId(UUID ownerId);
}
