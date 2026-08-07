package com.gymrats.api.workout.domain;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IdempotencyOperationRepository extends JpaRepository<IdempotencyOperation, UUID> {
  Optional<IdempotencyOperation> findByOwnerIdAndKey(UUID ownerId, String key);
}
