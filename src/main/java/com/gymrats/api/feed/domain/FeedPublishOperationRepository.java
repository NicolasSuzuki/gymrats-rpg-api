package com.gymrats.api.feed.domain;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeedPublishOperationRepository
    extends JpaRepository<FeedPublishOperation, UUID> {
  Optional<FeedPublishOperation> findByOwnerIdAndIdempotencyKey(
      UUID ownerId, String idempotencyKey);
}
